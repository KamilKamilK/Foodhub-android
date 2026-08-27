package pl.foodhub.pos.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TableDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String = "",
    @SerialName("number") val number: String = "",
    @SerialName("seats") val seats: Int = 0,
    @SerialName("roomId") val roomId: String? = null,
)

@Serializable
data class OccupiedTableDto(
    @SerialName("id") val id: Long,
    @SerialName("orderId") val orderId: String,
    @SerialName("tableId") val tableId: String,
)
