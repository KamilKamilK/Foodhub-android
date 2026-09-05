package pl.foodhub.pos.core.database

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * The offline write-ahead queue -- every sale/payment/table change is written locally
 * with a `type` + JSON payload and drained FIFO by `core:sync`'s worker when
 * connectivity is back (ANDROID_POS_ARCHITECTURE.md section 9 point 2). This interface
 * only knows about storage; it never interprets `type`/`payloadJson`.
 */
interface TransactionQueue {
    suspend fun enqueue(
        type: String,
        payloadJson: String,
    )

    /** The oldest still-pending operation, or null once the queue is drained. */
    suspend fun nextPending(): PendingOperation?

    suspend fun markSynced(id: Long)

    suspend fun markFailed(
        id: Long,
        error: String,
    )

    fun pendingCount(): Flow<Int>

    fun failedCount(): Flow<Int>

    data class PendingOperation(
        val id: Long,
        val type: String,
        val payloadJson: String,
    )
}

class RoomTransactionQueue
    @Inject
    constructor(
        private val dao: SyncOperationDao,
    ) : TransactionQueue {
        override suspend fun enqueue(
            type: String,
            payloadJson: String,
        ) {
            dao.insert(
                SyncOperationEntity(
                    type = type,
                    payloadJson = payloadJson,
                    createdAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun nextPending(): TransactionQueue.PendingOperation? =
            dao.nextPending()?.let { TransactionQueue.PendingOperation(it.id, it.type, it.payloadJson) }

        override suspend fun markSynced(id: Long) = dao.delete(id)

        override suspend fun markFailed(
            id: Long,
            error: String,
        ) = dao.markFailed(id, error)

        override fun pendingCount(): Flow<Int> = dao.observePendingCount()

        override fun failedCount(): Flow<Int> = dao.observeFailedCount()
    }
