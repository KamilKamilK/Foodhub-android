package pl.foodhub.pos.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PosMenuDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String = "",
    @SerialName("orientation") val orientation: String? = null,
)

@Serializable
data class PosMenuGroupDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String = "",
    @SerialName("position") val position: Int = 0,
    @SerialName("color") val color: String? = null,
)

@Serializable
data class PosMenuItemDto(
    @SerialName("id") val id: Long,
    @SerialName("groupId") val groupId: Long? = null,
    @SerialName("productId") val productId: String = "",
    @SerialName("productName") val productName: String = "",
    @SerialName("productType") val productType: String = "",
    @SerialName("position") val position: Int = 0,
    @SerialName("unitPriceGross") val unitPriceGross: Long = 0,
    @SerialName("taxRateValue") val taxRateValue: Double = 0.0,
)
