package pl.foodhub.pos.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Read-through cache of the room/table layout and its occupancy, mirroring
 * [MenuCacheDao]'s pattern: the tables screen renders from this snapshot and survives a
 * brief connectivity drop, while `foodhub-api` stays the source of truth -- a successful
 * refresh replaces the whole snapshot. [position] preserves the API's own ordering since
 * the table list carries no explicit sequence field of its own.
 */
@Entity(tableName = "table_cache")
data class TableCacheEntity(
    @PrimaryKey val id: String,
    val label: String,
    val seats: Int,
    val occupied: Boolean,
    val openOrderId: String?,
    val position: Int,
)

@Dao
interface TableCacheDao {
    @Query("SELECT * FROM table_cache ORDER BY position ASC")
    fun observeTables(): Flow<List<TableCacheEntity>>

    @Query("SELECT COUNT(*) FROM table_cache")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTables(tables: List<TableCacheEntity>)

    @Query("DELETE FROM table_cache")
    suspend fun clearTables()

    @Transaction
    suspend fun replaceSnapshot(tables: List<TableCacheEntity>) {
        clearTables()
        upsertTables(tables)
    }
}
