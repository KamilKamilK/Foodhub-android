package pl.foodhub.pos.feature.tables

import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.database.TableCacheDao
import pl.foodhub.pos.core.database.TableCacheEntity
import pl.foodhub.pos.core.network.api.TablesApi
import pl.foodhub.pos.core.network.model.OccupiedTableDto
import pl.foodhub.pos.core.network.model.TableDto
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
private class TestDispatcherProvider(dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()) :
    DispatcherProvider {
    override val io = dispatcher
    override val default = dispatcher
    override val main = dispatcher
}

class TablesRepositoryTest {
    private val tablesApi = mockk<TablesApi>()
    private val tableCacheDao = mockk<TableCacheDao> { every { observeTables() } returns flowOf(emptyList()) }
    private val repository = TablesRepository(tablesApi, tableCacheDao, TestDispatcherProvider())

    @Test
    fun `refresh replaces the cache with the fetched tables and their occupancy`() =
        runTest {
            coEvery { tablesApi.tables() } returns
                listOf(TableDto(id = "t1", name = "", number = "1", seats = 4))
            coEvery { tablesApi.occupiedTables() } returns
                listOf(OccupiedTableDto(id = 1, orderId = "o1", tableId = "t1"))
            coEvery { tableCacheDao.replaceSnapshot(any()) } just Runs

            val result = repository.refresh()

            assertTrue(result is ApiResult.Success)
            coVerify {
                tableCacheDao.replaceSnapshot(
                    withArg { snapshot ->
                        val row = snapshot.single()
                        assertTrue(row.id == "t1")
                        assertTrue(row.label == "Stolik 1") // blank name falls back to "Stolik <number>"
                        assertTrue(row.occupied)
                        assertTrue(row.openOrderId == "o1")
                    },
                )
            }
        }

    @Test
    fun `refresh leaves the cache untouched when the tables fetch fails`() =
        runTest {
            coEvery { tablesApi.tables() } throws IOException("offline")
            coEvery { tablesApi.occupiedTables() } returns emptyList()

            val result = repository.refresh()

            assertTrue(result is ApiResult.NetworkError)
            coVerify(exactly = 0) { tableCacheDao.replaceSnapshot(any()) }
        }

    @Test
    fun `hasCachedTables reflects whether the cache holds any rows`() =
        runTest {
            coEvery { tableCacheDao.count() } returns 0
            assertFalse(repository.hasCachedTables())

            coEvery { tableCacheDao.count() } returns 2
            assertTrue(repository.hasCachedTables())
        }

    @Test
    fun `tables maps cached rows into TableRow`() =
        runTest {
            val seededDao =
                mockk<TableCacheDao> {
                    every { observeTables() } returns
                        flowOf(
                            listOf(
                                TableCacheEntity(
                                    id = "t1",
                                    label = "Stolik 1",
                                    seats = 4,
                                    occupied = true,
                                    openOrderId = "o1",
                                    position = 0,
                                ),
                            ),
                        )
                }
            val seededRepository = TablesRepository(tablesApi, seededDao, TestDispatcherProvider())

            seededRepository.tables.test {
                val row = awaitItem().single()
                assertTrue(row.id == "t1")
                assertTrue(row.label == "Stolik 1")
                assertTrue(row.occupied)
                assertTrue(row.openOrderId == "o1")
                cancelAndIgnoreRemainingEvents()
            }
        }
}
