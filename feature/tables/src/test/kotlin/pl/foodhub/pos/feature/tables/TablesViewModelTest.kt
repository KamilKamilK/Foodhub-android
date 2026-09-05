package pl.foodhub.pos.feature.tables

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.foodhub.pos.core.auth.AuthRepository
import pl.foodhub.pos.core.auth.PosSession
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.sync.SyncQueue

@OptIn(ExperimentalCoroutinesApi::class)
class TablesViewModelTest {
    private val tablesRepository = mockk<TablesRepository>()
    private val authRepository = mockk<AuthRepository>()
    private val syncQueue = mockk<SyncQueue>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { authRepository.posSession } returns
            flowOf(PosSession(placeId = "place-1", placeName = "Bistro", posId = "pos-9"))
        every { tablesRepository.tables } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TablesViewModel(tablesRepository, authRepository, syncQueue)

    @Test
    fun `load populates tables and marks the occupied ones`() =
        runTest {
            every { tablesRepository.tables } returns
                flowOf(listOf(TableRow(id = "t1", label = "Stolik 1", seats = 4, occupied = true, openOrderId = "o1")))
            coEvery { tablesRepository.refresh() } returns ApiResult.Success(Unit)
            val viewModel = viewModel()

            viewModel.load()
            runCurrent()

            val table = viewModel.state.value.tables.single()
            assertFalse(viewModel.state.value.loading)
            assertTrue(table.occupied)
            assertEquals("o1", table.openOrderId)
        }

    @Test
    fun `failed load with no cache reports the empty-offline state`() =
        runTest {
            coEvery { tablesRepository.refresh() } returns ApiResult.NetworkError(RuntimeException("offline"))
            coEvery { tablesRepository.hasCachedTables() } returns false
            val viewModel = viewModel()

            viewModel.load()
            runCurrent()

            val state = viewModel.state.value
            assertFalse(state.loading)
            assertFalse(state.stale)
            assertTrue(state.emptyOffline)
        }

    @Test
    fun `failed load with an existing cache serves it stale instead of the empty-offline state`() =
        runTest {
            every { tablesRepository.tables } returns
                flowOf(listOf(TableRow(id = "t1", label = "Stolik 1", seats = 4, occupied = false, openOrderId = null)))
            coEvery { tablesRepository.refresh() } returns ApiResult.NetworkError(RuntimeException("offline"))
            coEvery { tablesRepository.hasCachedTables() } returns true
            val viewModel = viewModel()

            viewModel.load()
            runCurrent()

            val state = viewModel.state.value
            assertFalse(state.loading)
            assertTrue(state.stale)
            assertFalse(state.emptyOffline)
            assertEquals(1, state.tables.size)
        }

    @Test
    fun `selecting an occupied table resumes its existing order without queueing a new one`() =
        runTest {
            val viewModel = viewModel()
            val occupiedRow = TableRow(id = "t1", label = "Stolik 1", seats = 4, occupied = true, openOrderId = "o1")

            viewModel.openedTable.test {
                viewModel.selectTable(occupiedRow)
                assertEquals(TableSession(orderId = "o1", tableId = "t1"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            coVerify(exactly = 0) { syncQueue.createOrder(any(), any()) }
            coVerify(exactly = 0) { syncQueue.occupyTable(any(), any()) }
        }

    @Test
    fun `selecting a free table queues create-order and occupy, then opens it immediately`() =
        runTest {
            val viewModel = viewModel()
            val freeRow = TableRow(id = "t2", label = "Stolik 2", seats = 2, occupied = false, openOrderId = null)
            var openedOrderId: String? = null

            viewModel.openedTable.test {
                viewModel.selectTable(freeRow)
                val session = awaitItem()
                assertEquals("t2", session.tableId)
                openedOrderId = session.orderId
                cancelAndIgnoreRemainingEvents()
            }

            val orderIdSlot = slot<String>()
            coVerify { syncQueue.createOrder(capture(orderIdSlot), "place-1") }
            assertEquals(openedOrderId, orderIdSlot.captured)
            coVerify { syncQueue.occupyTable("t2", openedOrderId!!) }
        }
}
