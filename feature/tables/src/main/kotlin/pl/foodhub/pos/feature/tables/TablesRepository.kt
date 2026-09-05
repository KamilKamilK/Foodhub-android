package pl.foodhub.pos.feature.tables

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.database.TableCacheDao
import pl.foodhub.pos.core.database.TableCacheEntity
import pl.foodhub.pos.core.network.api.TablesApi
import pl.foodhub.pos.core.network.apiCall
import javax.inject.Inject

/**
 * Read-through: the UI observes the Room cache, [refresh] pulls the room layout and its
 * occupancy from `foodhub-api` and swaps the cache in atomically -- the same pattern
 * [pl.foodhub.pos.feature.menu.MenuRepository] uses for the menu.
 */
class TablesRepository
    @Inject
    constructor(
        private val tablesApi: TablesApi,
        private val tableCacheDao: TableCacheDao,
        private val dispatchers: DispatcherProvider,
    ) {
        val tables: Flow<List<TableRow>> =
            tableCacheDao.observeTables().map { cached ->
                cached.map {
                    TableRow(
                        id = it.id,
                        label = it.label,
                        seats = it.seats,
                        occupied = it.occupied,
                        openOrderId = it.openOrderId,
                    )
                }
            }

        suspend fun refresh(): ApiResult<Unit> =
            withContext(dispatchers.io) {
                val tables = apiCall { tablesApi.tables() }
                val occupied = apiCall { tablesApi.occupiedTables() }
                if (tables is ApiResult.Success && occupied is ApiResult.Success) {
                    val openByTable = occupied.value.associateBy({ it.tableId }, { it.orderId })
                    tableCacheDao.replaceSnapshot(
                        tables.value.mapIndexed { index, table ->
                            TableCacheEntity(
                                id = table.id,
                                label = table.name.ifBlank { "Stolik ${table.number}" },
                                seats = table.seats,
                                occupied = openByTable.containsKey(table.id),
                                openOrderId = openByTable[table.id],
                                position = index,
                            )
                        },
                    )
                    ApiResult.Success(Unit)
                } else {
                    (tables as? ApiResult.HttpError)
                        ?: (occupied as? ApiResult.HttpError)
                        ?: ApiResult.NetworkError(IllegalStateException("tables refresh failed"))
                }
            }

        suspend fun hasCachedTables(): Boolean = tableCacheDao.count() > 0
    }
