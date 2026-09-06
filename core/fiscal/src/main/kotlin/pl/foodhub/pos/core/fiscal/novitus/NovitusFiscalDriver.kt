package pl.foodhub.pos.core.fiscal.novitus

import pl.foodhub.pos.core.fiscal.FiscalManufacturer
import pl.foodhub.pos.core.fiscal.FiscalPrinterDriver
import pl.foodhub.pos.core.fiscal.model.FiscalCommitResult
import pl.foodhub.pos.core.fiscal.model.FiscalInvoiceRequest
import pl.foodhub.pos.core.fiscal.model.FiscalProbeResult
import pl.foodhub.pos.core.fiscal.model.FiscalReceiptRequest
import pl.foodhub.pos.core.fiscal.model.VatRate
import pl.foodhub.pos.core.fiscal.transport.FiscalTcpConnection
import pl.foodhub.pos.core.fiscal.transport.FiscalTcpConnectionFactory
import java.time.LocalDate
import javax.inject.Inject

private const val FRAME_END: Byte = 0x5C
private const val COMMAND_TIMEOUT_MS = 5_000L
private const val PROBE_TIMEOUT_MS = 2_000L
private const val STATUS_FRAME_LENGTH = 7

/**
 * Novitus fiscal-printer protocol driver: opens one connection per fiscalization
 * attempt and delegates the multi-step commit protocol to [NovitusCommitSession],
 * closing the connection whichever way the attempt ends.
 */
class NovitusFiscalDriver
    @Inject
    constructor(private val connectionFactory: FiscalTcpConnectionFactory) : FiscalPrinterDriver {
        override val manufacturer = FiscalManufacturer.NOVITUS

        override suspend fun probe(
            ip: String,
            port: Int,
        ): FiscalProbeResult {
            val connection = connectionFactory.connect(ip, port).getOrNull() ?: return FiscalProbeResult.Unreachable
            return connection.use { conn ->
                val response =
                    conn.exchangeFixedLength(
                        NovitusFrameCodec.enqProbe,
                        STATUS_FRAME_LENGTH,
                        PROBE_TIMEOUT_MS,
                    )
                val bytes = response.getOrNull() ?: return@use FiscalProbeResult.Unreachable
                when (NovitusFrameCodec.parseStatus(bytes)) {
                    is NovitusStatus.Ok -> FiscalProbeResult.Detected(FiscalManufacturer.NOVITUS)
                    else -> FiscalProbeResult.Ambiguous
                }
            }
        }

        override suspend fun fiscalizeReceipt(
            ip: String,
            port: Int,
            request: FiscalReceiptRequest,
        ): FiscalCommitResult {
            val connection = connectionFactory.connect(ip, port).getOrNull() ?: return FiscalCommitResult.Unreachable
            return connection.use { NovitusCommitSession(it).run(request) }
        }

        override suspend fun fiscalizeInvoice(
            ip: String,
            port: Int,
            request: FiscalInvoiceRequest,
        ): FiscalCommitResult {
            val connection = connectionFactory.connect(ip, port).getOrNull() ?: return FiscalCommitResult.Unreachable
            return connection.use { NovitusCommitSession(it).run(request.receipt) }
        }

        override suspend fun openDrawer(
            ip: String,
            port: Int,
        ): Result<Unit> {
            val connection = connectionFactory.connect(ip, port).getOrElse { return Result.failure(it) }
            return connection.use { conn ->
                conn.exchange(
                    NovitusFrameCodec.buildFrame(NovitusCommandBuilder.openDrawer()),
                    FRAME_END,
                    COMMAND_TIMEOUT_MS,
                )
                    .map { }
            }
        }

        override suspend fun dailyReport(
            ip: String,
            port: Int,
            cashierName: String,
        ): Result<Unit> {
            val connection = connectionFactory.connect(ip, port).getOrElse { return Result.failure(it) }
            return connection.use { conn ->
                val frame =
                    NovitusFrameCodec.buildFrame(
                        NovitusCommandBuilder.dailyReport(LocalDate.now(), cashierName),
                    )
                conn.exchange(frame, FRAME_END, COMMAND_TIMEOUT_MS).map { }
            }
        }

        override suspend fun monthlyReport(
            ip: String,
            port: Int,
            from: LocalDate,
            to: LocalDate,
            cashierName: String,
        ): Result<Unit> {
            val connection = connectionFactory.connect(ip, port).getOrElse { return Result.failure(it) }
            return connection.use { conn ->
                val frame = NovitusFrameCodec.buildFrame(NovitusCommandBuilder.monthlyReport(from, to, cashierName))
                conn.exchange(frame, FRAME_END, COMMAND_TIMEOUT_MS).map { }
            }
        }

        override suspend fun vatRates(
            ip: String,
            port: Int,
        ): Result<List<VatRate>> {
            val connection = connectionFactory.connect(ip, port).getOrElse { return Result.failure(it) }
            return connection.use { conn ->
                val frame = NovitusFrameCodec.buildFrame(NovitusCommandBuilder.fiscalInfo())
                val response =
                    conn.exchange(frame, FRAME_END, COMMAND_TIMEOUT_MS).getOrElse {
                            e ->
                        return@use Result.failure(e)
                    }
                Result.success(NovitusVatRateParser.parse(response))
            }
        }

        override suspend fun receiptCount(
            ip: String,
            port: Int,
        ): Result<Int> {
            val connection = connectionFactory.connect(ip, port).getOrElse { return Result.failure(it) }
            return connection.use { conn ->
                val frame = NovitusFrameCodec.buildFrame(NovitusCommandBuilder.fiscalInfo())
                val response =
                    conn.exchange(frame, FRAME_END, COMMAND_TIMEOUT_MS).getOrElse {
                            e ->
                        return@use Result.failure(e)
                    }
                val count = NovitusFrameCodec.extractDocumentNumber(response)?.toIntOrNull()
                count?.let { Result.success(it) }
                    ?: Result.failure(IllegalStateException("No receipt count in response."))
            }
        }
    }

/**
 * Owns one connection for the lifetime of a single fiscalization attempt:
 * cancels any dangling transaction defensively, then runs
 * begin -> per-line -> per-payment -> commit -> status poll. Split out of
 * [NovitusFiscalDriver] to keep the driver's own function count to its public
 * contract plus dispatch, and the commit protocol's steps each individually short.
 */
private class NovitusCommitSession(private val connection: FiscalTcpConnection) {
    suspend fun run(request: FiscalReceiptRequest): FiscalCommitResult {
        connection.exchange(
            NovitusFrameCodec.buildFrame(NovitusCommandBuilder.cancelTransaction()),
            FRAME_END,
            COMMAND_TIMEOUT_MS,
        )

        val beginFrame = NovitusFrameCodec.buildFrame(NovitusCommandBuilder.beginTransaction(request.buyerNip))
        if (connection.exchange(
                beginFrame,
                FRAME_END,
                COMMAND_TIMEOUT_MS,
            ).isFailure
        ) {
            return FiscalCommitResult.Unreachable
        }

        val linesAndPaymentsSent = sendLines(request) && sendPayments(request)
        if (!linesAndPaymentsSent) return FiscalCommitResult.Unreachable

        val commitResponse = sendCommit(request) ?: return FiscalCommitResult.Unreachable
        val documentNumber = NovitusFrameCodec.extractDocumentNumber(commitResponse)
        val status = pollStatus() ?: return FiscalCommitResult.Ambiguous

        return resultFor(status, documentNumber)
    }

    private suspend fun sendLines(request: FiscalReceiptRequest): Boolean {
        for (line in request.lines) {
            val frame = NovitusFrameCodec.buildFrame(NovitusCommandBuilder.printLine(line))
            if (connection.exchange(frame, FRAME_END, COMMAND_TIMEOUT_MS).isFailure) return false
        }
        return true
    }

    private suspend fun sendPayments(request: FiscalReceiptRequest): Boolean {
        for (payment in request.payments) {
            val frame = NovitusFrameCodec.buildFrame(NovitusCommandBuilder.addPayment(payment))
            if (connection.exchange(frame, FRAME_END, COMMAND_TIMEOUT_MS).isFailure) return false
        }
        return true
    }

    private suspend fun sendCommit(request: FiscalReceiptRequest): ByteArray? {
        val total = request.lines.sumOf { it.totalGrosz }
        val paid = request.payments.sumOf { it.amountGrosz }
        val frame = NovitusFrameCodec.buildFrame(NovitusCommandBuilder.confirmPayment("", total, paid))
        return connection.exchange(frame, FRAME_END, COMMAND_TIMEOUT_MS).getOrNull()
    }

    private suspend fun pollStatus(): NovitusStatus? {
        val result = connection.exchangeFixedLength(NovitusFrameCodec.enqProbe, STATUS_FRAME_LENGTH, COMMAND_TIMEOUT_MS)
        val response = result.getOrNull() ?: return null
        return NovitusFrameCodec.parseStatus(response)
    }

    private suspend fun resultFor(
        status: NovitusStatus,
        documentNumber: String?,
    ): FiscalCommitResult =
        when (status) {
            is NovitusStatus.Ok -> resultForOk(status, documentNumber)
            is NovitusStatus.Error ->
                FiscalCommitResult.DeviceError(
                    status.code.toString(),
                    NovitusErrorCodes.messageFor(status.code),
                )
            NovitusStatus.MechanismError -> FiscalCommitResult.DeviceError(null, "Błąd mechanizmu drukarki fiskalnej.")
            NovitusStatus.Ambiguous -> resultForAmbiguous()
        }

    private suspend fun resultForOk(
        status: NovitusStatus.Ok,
        documentNumber: String?,
    ): FiscalCommitResult {
        if (!status.lastTransactionCorrect || documentNumber == null) return FiscalCommitResult.Ambiguous
        connection.exchange(
            NovitusFrameCodec.buildFrame(NovitusCommandBuilder.openDrawer()),
            FRAME_END,
            COMMAND_TIMEOUT_MS,
        )
        return FiscalCommitResult.Success(fiscalDocumentNumber = documentNumber, dailyReportNumber = null)
    }

    private suspend fun resultForAmbiguous(): FiscalCommitResult {
        val frame = NovitusFrameCodec.buildFrame(NovitusCommandBuilder.lastError())
        val detail =
            connection.exchange(
                frame,
                FRAME_END,
                COMMAND_TIMEOUT_MS,
            ).getOrNull()?.let(NovitusFrameCodec::parseStatus)
        return if (detail is NovitusStatus.Error) {
            FiscalCommitResult.DeviceError(detail.code.toString(), NovitusErrorCodes.messageFor(detail.code))
        } else {
            FiscalCommitResult.Ambiguous
        }
    }
}
