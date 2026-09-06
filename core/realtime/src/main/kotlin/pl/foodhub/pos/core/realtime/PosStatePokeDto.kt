package pl.foodhub.pos.core.realtime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Wire shape published on `places/{placeId}/pos-state` (foodhub-api's PosStatePokePublisherInterface). */
@Serializable
private data class PosStatePokeDto(
    @SerialName("resource") val resource: String,
    @SerialName("placeId") val placeId: String,
)

private val json = Json { ignoreUnknownKeys = true }

/**
 * Parses a raw Mercure message into a [RealtimeEvent], or null if it isn't a
 * recognised poke -- a forward-compatible unknown `resource` (a future backend
 * addition this build predates) is ignored rather than crashing the subscriber.
 */
fun parsePosStatePoke(data: String): RealtimeEvent? =
    runCatching { json.decodeFromString<PosStatePokeDto>(data) }
        .getOrNull()
        ?.let { poke ->
            when (poke.resource) {
                "occupied-tables" -> RealtimeEvent.OCCUPIED_TABLES
                "receipt-issued" -> RealtimeEvent.RECEIPT_ISSUED
                "invoice-issued" -> RealtimeEvent.INVOICE_ISSUED
                "order-created" -> RealtimeEvent.ORDER_CREATED
                else -> null
            }
        }
