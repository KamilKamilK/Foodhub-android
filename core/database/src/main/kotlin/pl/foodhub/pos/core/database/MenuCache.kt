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
 * Read-through cache of the menu/price list so the sales screen renders instantly and
 * survives a brief connectivity drop. The terminal never mutates these rows -- the
 * source of truth is `foodhub-api`; a Mercure "poke" or the refresh interval replaces
 * the whole snapshot (ANDROID_POS_ARCHITECTURE.md section 9 point 1).
 */
@Entity(tableName = "menu_group")
data class MenuGroupEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val position: Int,
)

@Entity(tableName = "menu_item")
data class MenuItemEntity(
    @PrimaryKey val id: Long,
    val groupId: Long?,
    val productId: String,
    val productName: String,
    val productType: String,
    val position: Int,
    val unitPriceGrossMinor: Long,
    val taxRateValue: Double,
)

@Dao
interface MenuCacheDao {
    @Query("SELECT * FROM menu_group ORDER BY position ASC")
    fun observeGroups(): Flow<List<MenuGroupEntity>>

    @Query("SELECT * FROM menu_item ORDER BY position ASC")
    fun observeItems(): Flow<List<MenuItemEntity>>

    @Query("SELECT COUNT(*) FROM menu_item")
    suspend fun itemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroups(groups: List<MenuGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<MenuItemEntity>)

    @Query("DELETE FROM menu_group")
    suspend fun clearGroups()

    @Query("DELETE FROM menu_item")
    suspend fun clearItems()

    @Transaction
    suspend fun replaceSnapshot(
        groups: List<MenuGroupEntity>,
        items: List<MenuItemEntity>,
    ) {
        clearGroups()
        clearItems()
        upsertGroups(groups)
        upsertItems(items)
    }
}
