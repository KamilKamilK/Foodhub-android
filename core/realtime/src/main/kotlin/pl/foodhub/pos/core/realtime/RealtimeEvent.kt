package pl.foodhub.pos.core.realtime

/**
 * A Mercure "poke" for `places/{placeId}/pos-state" (ANDROID_POS_ARCHITECTURE.md
 * section 4/14 Faza 4): a hint that something changed, never entity data -- a
 * receiver reacts by refreshing through the normal REST repositories, which stay the
 * source of truth.
 */
enum class RealtimeEvent {
    OCCUPIED_TABLES,
    RECEIPT_ISSUED,
    INVOICE_ISSUED,
    ORDER_CREATED,
}
