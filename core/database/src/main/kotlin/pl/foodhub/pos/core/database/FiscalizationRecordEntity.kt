package pl.foodhub.pos.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * One receipt's/invoice's fiscal-memory commit outcome, keyed by the document id
 * already assigned to it ([documentId] -- a globally-unique client-generated UUID,
 * see `feature:sales`' `SalesRepository`). This is the crash-safe idempotency guard
 * a fiscal hardware commit needs and the offline sync queue's client-generated-id
 * trick alone cannot provide: resending a fiscal-memory commit command fiscalizes
 * the sale a second time, an irreversible legal document, so [FiscalizationLedger]
 * must answer "did this already happen" before a driver is ever invoked twice for
 * the same document -- including across an app kill/restart.
 */
@Entity(tableName = "fiscalization_record")
data class FiscalizationRecordEntity(
    @PrimaryKey val documentId: String,
    val documentType: String,
    val fiscalDeviceId: String,
    val fiscalDocumentNumber: String,
    val dailyReportNumber: String?,
    val fiscalizedAtEpochMs: Long,
)

object FiscalDocumentType {
    const val RECEIPT = "RECEIPT"
    const val INVOICE = "INVOICE"
}

@Dao
interface FiscalizationRecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: FiscalizationRecordEntity)

    @Query("SELECT * FROM fiscalization_record WHERE documentId = :documentId")
    suspend fun findByDocumentId(documentId: String): FiscalizationRecordEntity?
}
