package pl.foodhub.pos.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A single queued write, FIFO by [id]. `type` and `payloadJson` are opaque to this
 * module on purpose -- this module never needs to know what a "confirm order" or
 * "occupy table" operation looks like, only how to store and drain a FIFO queue of
 * them (ANDROID_POS_ARCHITECTURE.md section 9 point 2). `core:sync` owns the actual
 * operation types and payload shapes.
 */
@Entity(tableName = "sync_operation")
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val payloadJson: String,
    val createdAtEpochMs: Long,
    val status: String = SyncOperationStatus.PENDING,
    val lastError: String? = null,
)

object SyncOperationStatus {
    const val PENDING = "PENDING"
    const val FAILED = "FAILED"
}

@Dao
interface SyncOperationDao {
    @Insert
    suspend fun insert(operation: SyncOperationEntity): Long

    @Query("SELECT * FROM sync_operation WHERE status = 'PENDING' ORDER BY id ASC LIMIT 1")
    suspend fun nextPending(): SyncOperationEntity?

    @Query("DELETE FROM sync_operation WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE sync_operation SET status = 'FAILED', lastError = :error WHERE id = :id")
    suspend fun markFailed(
        id: Long,
        error: String,
    )

    @Query("SELECT COUNT(*) FROM sync_operation WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_operation WHERE status = 'FAILED'")
    fun observeFailedCount(): Flow<Int>
}
