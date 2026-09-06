package pl.foodhub.pos.core.fiscal

import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.database.FiscalDocumentType
import pl.foodhub.pos.core.database.FiscalizationLedger
import pl.foodhub.pos.core.database.FiscalizationRecordEntity
import pl.foodhub.pos.core.fiscal.model.FiscalCommitResult
import pl.foodhub.pos.core.fiscal.model.FiscalInvoiceRequest
import pl.foodhub.pos.core.fiscal.model.FiscalReceiptRequest
import pl.foodhub.pos.core.network.api.FiscalDeviceApi
import pl.foodhub.pos.core.network.apiCall
import pl.foodhub.pos.core.network.model.FiscalDeviceDto
import javax.inject.Inject

private const val HTTP_NOT_FOUND = 404

private sealed interface DeviceResolution {
    data class Ready(val device: FiscalDeviceDto, val driver: FiscalPrinterDriver, val port: Int) : DeviceResolution

    data class Failed(val outcome: FiscalOutcome) : DeviceResolution
}

/**
 * Orchestrates fiscalizing one document: checks the local [FiscalizationLedger]
 * first (a document can only ever be fiscalized once -- see
 * [pl.foodhub.pos.core.database.FiscalizationRecordEntity] for why this must survive
 * an app kill/restart), then the configured [pl.foodhub.pos.core.network.model
 * .FiscalDeviceDto] for the terminal's Pos, then the matching
 * [FiscalPrinterDriver]. Writes to the ledger only on a confirmed hardware success,
 * so a failed attempt's retry safely re-attempts the hardware step.
 */
class FiscalCoordinator
    @Inject
    constructor(
        private val fiscalDeviceApi: FiscalDeviceApi,
        private val driverFactory: FiscalDriverFactory,
        private val ledger: FiscalizationLedger,
    ) {
        suspend fun fiscalizeReceipt(
            placeId: String,
            posId: String,
            receiptId: String,
            request: FiscalReceiptRequest,
        ): FiscalOutcome =
            fiscalize(placeId, posId, receiptId, FiscalDocumentType.RECEIPT) { device, driver, port ->
                driver.fiscalizeReceipt(device.ip, port, request)
            }

        suspend fun fiscalizeInvoice(
            placeId: String,
            posId: String,
            invoiceId: String,
            request: FiscalInvoiceRequest,
        ): FiscalOutcome =
            fiscalize(placeId, posId, invoiceId, FiscalDocumentType.INVOICE) { device, driver, port ->
                driver.fiscalizeInvoice(device.ip, port, request)
            }

        private suspend fun fiscalize(
            placeId: String,
            posId: String,
            documentId: String,
            documentType: String,
            commit: suspend (FiscalDeviceDto, FiscalPrinterDriver, Int) -> FiscalCommitResult,
        ): FiscalOutcome {
            ledger.findByDocumentId(documentId)?.let {
                return FiscalOutcome.Fiscalized(it.fiscalDeviceId, it.fiscalDocumentNumber, it.dailyReportNumber)
            }

            val resolution = resolveDevice(placeId, posId)
            if (resolution is DeviceResolution.Failed) return resolution.outcome
            val (device, driver, port) = resolution as DeviceResolution.Ready

            return when (val result = commit(device, driver, port)) {
                is FiscalCommitResult.Success -> {
                    ledger.record(
                        FiscalizationRecordEntity(
                            documentId = documentId,
                            documentType = documentType,
                            fiscalDeviceId = device.id,
                            fiscalDocumentNumber = result.fiscalDocumentNumber,
                            dailyReportNumber = result.dailyReportNumber,
                            fiscalizedAtEpochMs = System.currentTimeMillis(),
                        ),
                    )
                    FiscalOutcome.Fiscalized(device.id, result.fiscalDocumentNumber, result.dailyReportNumber)
                }
                is FiscalCommitResult.DeviceError -> FiscalOutcome.Failed(result.message)
                FiscalCommitResult.Unreachable -> FiscalOutcome.Failed("Kasa fiskalna nieosiągalna.")
                FiscalCommitResult.Ambiguous -> FiscalOutcome.Failed("Niejednoznaczna odpowiedź kasy fiskalnej.")
            }
        }

        private suspend fun resolveDevice(
            placeId: String,
            posId: String,
        ): DeviceResolution {
            val device =
                when (val result = apiCall { fiscalDeviceApi.fiscalDevice(placeId, posId) }) {
                    is ApiResult.Success -> result.value
                    is ApiResult.HttpError ->
                        return DeviceResolution.Failed(
                            if (result.status == HTTP_NOT_FOUND) {
                                FiscalOutcome.NotConfigured
                            } else {
                                FiscalOutcome.Failed("Nie udało się pobrać konfiguracji kasy fiskalnej.")
                            },
                        )
                    is ApiResult.NetworkError -> return DeviceResolution.Failed(
                        FiscalOutcome.Failed("Brak połączenia z serwerem."),
                    )
                }

            val manufacturer =
                FiscalManufacturer.entries.find { it.name == device.manufacturer } ?: run {
                    val reason = "Nieznany producent kasy fiskalnej: ${device.manufacturer}."
                    return DeviceResolution.Failed(FiscalOutcome.Failed(reason))
                }
            val port =
                device.port.toIntOrNull()
                    ?: return DeviceResolution.Failed(FiscalOutcome.Failed("Nieprawidłowy port kasy fiskalnej."))

            return DeviceResolution.Ready(device, driverFactory.forManufacturer(manufacturer), port)
        }
    }
