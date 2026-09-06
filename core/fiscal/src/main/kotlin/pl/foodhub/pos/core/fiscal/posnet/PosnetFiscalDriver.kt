package pl.foodhub.pos.core.fiscal.posnet

import pl.foodhub.pos.core.fiscal.FiscalManufacturer
import pl.foodhub.pos.core.fiscal.FiscalPrinterDriver
import pl.foodhub.pos.core.fiscal.model.FiscalCommitResult
import pl.foodhub.pos.core.fiscal.model.FiscalInvoiceRequest
import pl.foodhub.pos.core.fiscal.model.FiscalProbeResult
import pl.foodhub.pos.core.fiscal.model.FiscalReceiptRequest
import pl.foodhub.pos.core.fiscal.model.VatRate
import pl.foodhub.pos.core.fiscal.transport.FiscalTcpConnection
import pl.foodhub.pos.core.fiscal.transport.FiscalTcpConnectionFactory
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

private const val COMMAND_TIMEOUT_MS = 5_000L
private const val PROBE_TIMEOUT_MS = 2_000L
private const val UNKNOWN_CASHIER = ""
private const val FEED_LINES = 4
private const val DOCUMENT_NUMBER_FIELD = "bt"
private const val RECEIPT_COUNT_FIELD = "rd"

/**
 * Posnet online fiscal-printer protocol driver: opens one connection per
 * fiscalization attempt and delegates the multi-step commit protocol to
 * [PosnetCommitSession], closing the connection whichever way the attempt ends.
 */
class PosnetFiscalDriver
    @Inject
    constructor(private val connectionFactory: FiscalTcpConnectionFactory) : FiscalPrinterDriver {
        override val manufacturer = FiscalManufacturer.POSNET_ONLINE

        override suspend fun probe(
            ip: String,
            port: Int,
        ): FiscalProbeResult {
            val connection = connectionFactory.connect(ip, port).getOrNull() ?: return FiscalProbeResult.Unreachable
            return connection.use { conn ->
                val frame = PosnetFrameCodec.buildFrame(PosnetCommandBuilder.deviceStatus())
                val response =
                    conn.exchange(frame, PosnetFrameCodec.END_MARKER, PROBE_TIMEOUT_MS).getOrNull()
                        ?: return@use FiscalProbeResult.Unreachable
                when (PosnetFrameCodec.parse(response)) {
                    is PosnetResponse.Fields -> FiscalProbeResult.Detected(FiscalManufacturer.POSNET_ONLINE)
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
            return connection.use { PosnetCommitSession(it).runReceipt(request) }
        }

        override suspend fun fiscalizeInvoice(
            ip: String,
            port: Int,
            request: FiscalInvoiceRequest,
        ): FiscalCommitResult {
            val connection = connectionFactory.connect(ip, port).getOrNull() ?: return FiscalCommitResult.Unreachable
            return connection.use { PosnetCommitSession(it).runInvoice(request) }
        }

        override suspend fun openDrawer(
            ip: String,
            port: Int,
        ): Result<Unit> {
            val connection = connectionFactory.connect(ip, port).getOrElse { return Result.failure(it) }
            return connection.use { conn ->
                val frame = PosnetFrameCodec.buildFrame(PosnetCommandBuilder.openDrawer())
                conn.exchange(frame, PosnetFrameCodec.END_MARKER, COMMAND_TIMEOUT_MS).map { }
            }
        }

        override suspend fun dailyReport(
            ip: String,
            port: Int,
            cashierName: String,
        ): Result<Unit> {
            val connection = connectionFactory.connect(ip, port).getOrElse { return Result.failure(it) }
            return connection.use { conn ->
                val frame = PosnetFrameCodec.buildFrame(PosnetCommandBuilder.dailyReport(LocalDate.now()))
                conn.exchange(frame, PosnetFrameCodec.END_MARKER, COMMAND_TIMEOUT_MS).map { }
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
                val frame = PosnetFrameCodec.buildFrame(PosnetCommandBuilder.monthlyReport(to))
                conn.exchange(frame, PosnetFrameCodec.END_MARKER, COMMAND_TIMEOUT_MS).map { }
            }
        }

        override suspend fun vatRates(
            ip: String,
            port: Int,
        ): Result<List<VatRate>> {
            val connection = connectionFactory.connect(ip, port).getOrElse { return Result.failure(it) }
            return connection.use { conn ->
                val frame = PosnetFrameCodec.buildFrame(PosnetCommandBuilder.vatRates())
                val response =
                    conn.exchange(frame, PosnetFrameCodec.END_MARKER, COMMAND_TIMEOUT_MS).getOrElse {
                            e ->
                        return@use Result.failure(e)
                    }
                when (val parsed = PosnetFrameCodec.parse(response)) {
                    is PosnetResponse.Fields -> Result.success(PosnetVatRateParser.parse(parsed.values))
                    else -> Result.failure(IllegalStateException("Unexpected VAT rate response."))
                }
            }
        }

        override suspend fun receiptCount(
            ip: String,
            port: Int,
        ): Result<Int> {
            val connection = connectionFactory.connect(ip, port).getOrElse { return Result.failure(it) }
            return connection.use { conn ->
                val frame = PosnetFrameCodec.buildFrame(PosnetCommandBuilder.documentCounters())
                val response =
                    conn.exchange(frame, PosnetFrameCodec.END_MARKER, COMMAND_TIMEOUT_MS).getOrElse {
                            e ->
                        return@use Result.failure(e)
                    }
                val fields =
                    PosnetFrameCodec.parse(response) as? PosnetResponse.Fields
                        ?: return@use Result.failure(IllegalStateException("Unexpected document-counter response."))
                val count = fields.values[RECEIPT_COUNT_FIELD]?.toIntOrNull()
                count?.let { Result.success(it) }
                    ?: Result.failure(IllegalStateException("No receipt count in response."))
            }
        }
    }

/**
 * Owns one connection for the lifetime of a single fiscalization attempt. Unlike
 * Novitus, the defensive `prncancel` is issued at the END of a successful flow, not
 * the start -- this is the documented Posnet convention, not a bug; do not "fix" it
 * to match Novitus's ordering. Split out of [PosnetFiscalDriver] to keep the
 * driver's own function count to its public contract plus dispatch, and the commit
 * protocol's steps each individually short.
 */
private class PosnetCommitSession(private val connection: FiscalTcpConnection) {
    suspend fun runReceipt(request: FiscalReceiptRequest): FiscalCommitResult = commit(request, isInvoice = false)

    suspend fun runInvoice(request: FiscalInvoiceRequest): FiscalCommitResult {
        val buyerTokens = PosnetCommandBuilder.invoiceBuyer(request.buyerName, request.buyerNip, request.buyerAddress)
        val headerSent = send(PosnetCommandBuilder.initInvoice()) != null && send(buyerTokens) != null
        if (!headerSent) return FiscalCommitResult.Unreachable
        return commit(request.receipt, isInvoice = true)
    }

    private suspend fun commit(
        request: FiscalReceiptRequest,
        isInvoice: Boolean,
    ): FiscalCommitResult {
        val initFrame = PosnetFrameCodec.buildFrame(PosnetCommandBuilder.initTransaction())
        if (connection.exchange(initFrame, PosnetFrameCodec.END_MARKER, COMMAND_TIMEOUT_MS).isFailure) {
            return FiscalCommitResult.Unreachable
        }

        val bodySent = sendLines(request) && sendPayments(request) && sendNip(request)
        if (!bodySent) return FiscalCommitResult.Unreachable

        val footerFrame = PosnetFrameCodec.buildFrame(PosnetCommandBuilder.setFooter(UNKNOWN_CASHIER))
        connection.exchange(footerFrame, PosnetFrameCodec.END_MARKER, COMMAND_TIMEOUT_MS)

        val endResponse = sendEndTransaction(request, isInvoice) ?: return FiscalCommitResult.Unreachable
        return resultFor(endResponse)
    }

    private suspend fun sendLines(request: FiscalReceiptRequest): Boolean {
        for (line in request.lines) {
            val frame = PosnetFrameCodec.buildFrame(PosnetCommandBuilder.line(line))
            if (connection.exchange(frame, PosnetFrameCodec.END_MARKER, COMMAND_TIMEOUT_MS).isFailure) return false
        }
        return true
    }

    private suspend fun sendPayments(request: FiscalReceiptRequest): Boolean {
        for (payment in request.payments) {
            val frame = PosnetFrameCodec.buildFrame(PosnetCommandBuilder.payment(payment))
            if (connection.exchange(frame, PosnetFrameCodec.END_MARKER, COMMAND_TIMEOUT_MS).isFailure) return false
        }
        return true
    }

    private suspend fun sendNip(request: FiscalReceiptRequest): Boolean {
        val nip = request.buyerNip ?: return true
        val frame = PosnetFrameCodec.buildFrame(PosnetCommandBuilder.setNip(nip))
        return connection.exchange(frame, PosnetFrameCodec.END_MARKER, COMMAND_TIMEOUT_MS).isSuccess
    }

    private suspend fun sendEndTransaction(
        request: FiscalReceiptRequest,
        isInvoice: Boolean,
    ): PosnetResponse? {
        val total = request.lines.sumOf { it.totalGrosz }
        val paid = request.payments.sumOf { it.amountGrosz }
        val endFrame = PosnetFrameCodec.buildFrame(PosnetCommandBuilder.endTransaction(total, paid, isInvoice))
        val exchangeResult = connection.exchange(endFrame, PosnetFrameCodec.END_MARKER, COMMAND_TIMEOUT_MS)
        val response = exchangeResult.getOrNull() ?: return null
        return PosnetFrameCodec.parse(response)
    }

    private suspend fun resultFor(parsed: PosnetResponse): FiscalCommitResult =
        when (parsed) {
            is PosnetResponse.Error ->
                FiscalCommitResult.DeviceError(
                    parsed.code,
                    PosnetErrorCodes.messageFor(parsed.code),
                )
            is PosnetResponse.Fields -> resultForFields(parsed)
            PosnetResponse.Ambiguous -> FiscalCommitResult.Ambiguous
        }

    private suspend fun resultForFields(parsed: PosnetResponse.Fields): FiscalCommitResult {
        val documentNumber = parsed.values[DOCUMENT_NUMBER_FIELD]
        if (documentNumber.isNullOrEmpty()) return FiscalCommitResult.Ambiguous

        connection.exchange(
            PosnetFrameCodec.buildFrame(PosnetCommandBuilder.feed(FEED_LINES)),
            PosnetFrameCodec.END_MARKER,
            COMMAND_TIMEOUT_MS,
        )
        connection.exchange(
            PosnetFrameCodec.buildFrame(PosnetCommandBuilder.openDrawer()),
            PosnetFrameCodec.END_MARKER,
            COMMAND_TIMEOUT_MS,
        )
        connection.exchange(
            PosnetFrameCodec.buildFrame(PosnetCommandBuilder.cancelTransaction()),
            PosnetFrameCodec.END_MARKER,
            COMMAND_TIMEOUT_MS,
        )
        return FiscalCommitResult.Success(fiscalDocumentNumber = documentNumber, dailyReportNumber = null)
    }

    private suspend fun send(tokens: List<String>): PosnetResponse? {
        val frame = PosnetFrameCodec.buildFrame(tokens)
        val result = connection.exchange(frame, PosnetFrameCodec.END_MARKER, COMMAND_TIMEOUT_MS)
        val response = result.getOrNull() ?: return null
        return PosnetFrameCodec.parse(response)
    }
}

private object PosnetErrorCodes {
    fun messageFor(code: String): String = "Błąd drukarki fiskalnej Posnet (kod $code)."
}

private object PosnetVatRateParser {
    private val VAT_SLOT_KEYS = listOf("v0", "v1", "v2", "v3", "v4", "v5", "v6")

    fun parse(fields: Map<String, String>): List<VatRate> =
        VAT_SLOT_KEYS.mapIndexedNotNull { index, key ->
            fields[key]?.toBigDecimalOrNull()?.let { VatRate(index, it) }
        }

    private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()
}
