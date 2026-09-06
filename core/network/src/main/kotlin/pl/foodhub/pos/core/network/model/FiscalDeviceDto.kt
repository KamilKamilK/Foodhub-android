package pl.foodhub.pos.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FiscalDeviceDto(
    @SerialName("id") val id: String,
    @SerialName("placeId") val placeId: String,
    @SerialName("posId") val posId: String,
    @SerialName("name") val name: String = "",
    @SerialName("manufacturer") val manufacturer: String,
    @SerialName("ip") val ip: String,
    @SerialName("port") val port: String,
    @SerialName("registrationNumber") val registrationNumber: String? = null,
)

@Serializable
data class FiscalizeReceiptRequestDto(
    @SerialName("fiscalDeviceId") val fiscalDeviceId: String,
    @SerialName("fiscalDocumentNumber") val fiscalDocumentNumber: String,
    @SerialName("dailyReportNumber") val dailyReportNumber: String? = null,
    @SerialName("fiscalizedAt") val fiscalizedAt: String,
)

@Serializable
data class FiscalizeInvoiceRequestDto(
    @SerialName("fiscalDeviceId") val fiscalDeviceId: String,
    @SerialName("fiscalDocumentNumber") val fiscalDocumentNumber: String,
    @SerialName("dailyReportNumber") val dailyReportNumber: String? = null,
    @SerialName("fiscalizedAt") val fiscalizedAt: String,
)
