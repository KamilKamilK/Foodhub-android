package pl.foodhub.pos.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.foodhub.pos.core.database.FoodHubPosDatabase
import pl.foodhub.pos.core.database.MenuCacheDao
import pl.foodhub.pos.core.database.RoomTransactionQueue
import pl.foodhub.pos.core.database.SyncOperationDao
import pl.foodhub.pos.core.database.TransactionQueue
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): FoodHubPosDatabase =
        Room.databaseBuilder(context, FoodHubPosDatabase::class.java, "foodhub-pos.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun menuCacheDao(database: FoodHubPosDatabase): MenuCacheDao = database.menuCacheDao()

    @Provides
    fun syncOperationDao(database: FoodHubPosDatabase): SyncOperationDao = database.syncOperationDao()

    @Provides
    @Singleton
    fun transactionQueue(dao: SyncOperationDao): TransactionQueue = RoomTransactionQueue(dao)
}
