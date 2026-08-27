package pl.foodhub.pos.core.database

/**
 * Faza 2 placeholder. The offline write-ahead queue -- every sale/payment/table
 * change written locally with a client UUID and a `pending` status, drained FIFO by
 * WorkManager when connectivity and a valid JWT are back (ANDROID_POS_ARCHITECTURE.md
 * section 9 point 2). Faza 1 is online-only, so nothing implements this yet.
 */
interface TransactionQueue {
    suspend fun enqueue(operation: PendingOperation)

    suspend fun pending(): List<PendingOperation>

    suspend fun markSynced(clientId: String)

    data class PendingOperation(
        val clientId: String,
        val type: String,
        val payloadJson: String,
        val createdAtEpochMs: Long,
    )
}
