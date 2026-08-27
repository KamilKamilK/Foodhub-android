package pl.foodhub.pos.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MenuGroupEntity::class,
        MenuItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FoodHubPosDatabase : RoomDatabase() {
    abstract fun menuCacheDao(): MenuCacheDao
}
