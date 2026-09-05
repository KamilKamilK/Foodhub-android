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

/**
 * `orderId` is the table's actual current occupant, which may differ from the orderId
 * the caller requested -- `conflict` is true when it does (someone else's occupy won
 * the race). core:sync's SyncProcessor treats a conflict as a synced no-op, not a
 * failure: the requester's order still exists independently, it just isn't holding
 * this table, and `TablesViewModel.load()` self-corrects from server truth on its next
 * read (ANDROID_POS_ARCHITECTURE.md section 9 point 4).
 */
@Serializable
data class OccupyTableResponseDto(
    @SerialName("tableId") val tableId: String,
    @SerialName("orderId") val orderId: String,
    @SerialName("conflict") val conflict: Boolean,
)
