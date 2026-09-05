package pl.foodhub.pos.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrinterDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String = "",
    @SerialName("ip") val ip: String = "",
    @SerialName("port") val port: String = "",
    @SerialName("role") val role: String = "KITCHEN",
    @SerialName("orderDirectionIds") val orderDirectionIds: List<Long> = emptyList(),
)
