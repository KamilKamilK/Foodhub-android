package pl.foodhub.pos.feature.tables

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import pl.foodhub.pos.core.network.api.SalesApi
import pl.foodhub.pos.core.network.api.TablesApi
import pl.foodhub.pos.core.network.model.CreateOrderRequestDto
import pl.foodhub.pos.core.network.model.OccupiedTableDto
import pl.foodhub.pos.core.network.model.OrderDto
import pl.foodhub.pos.core.network.model.TableDto

@OptIn(ExperimentalCoroutinesApi::class)
class TablesViewModelTest {
    private val tablesApi = mockk<TablesApi>()
    private val salesApi = mockk<SalesApi>()
    private val authRepository = mockk<AuthRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { authRepository.posSession } returns
            flowOf(PosSession(placeId = "place-1", placeName = "Bistro", posId = "pos-9"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TablesViewModel(tablesApi, salesApi, authRepository)

    @Test
    fun `load populates tables and marks the occupied ones`() =
        runTest {
            coEvery { tablesApi.tables() } returns
                listOf(TableDto(id = "t1", name = "Stolik 1", number = "1", seats = 4))
            coEvery { tablesApi.occupiedTables() } returns
                listOf(OccupiedTableDto(id = 1, orderId = "o1", tableId = "t1"))
            val viewModel = viewModel()

            viewModel.load()
            runCurrent()

            val table = viewModel.state.value.tables.single()
            assertFalse(viewModel.state.value.loading)
            assertTrue(table.occupied)
            assertEquals("o1", table.openOrderId)
        }

    @Test
    fun `selecting an occupied table resumes its existing order without creating a new one`() =
        runTest {
            val viewModel = viewModel()
            val occupiedRow = TableRow(id = "t1", label = "Stolik 1", seats = 4, occupied = true, openOrderId = "o1")

            viewModel.openedTable.test {
                viewModel.selectTable(occupiedRow)
                assertEquals(TableSession(orderId = "o1", tableId = "t1"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            coVerify(exactly = 0) { salesApi.createOrder(any()) }
        }

    @Test
    fun `selecting a free table creates and occupies an order before opening it`() =
        runTest {
            coEvery { salesApi.createOrder(CreateOrderRequestDto(placeId = "place-1")) } returns OrderDto(id = "o2")
            coEvery { tablesApi.occupy("t2", "o2") } returns Unit
            val viewModel = viewModel()
            val freeRow = TableRow(id = "t2", label = "Stolik 2", seats = 2, occupied = false, openOrderId = null)

            viewModel.openedTable.test {
                viewModel.selectTable(freeRow)
                assertEquals(TableSession(orderId = "o2", tableId = "t2"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            coVerify { tablesApi.occupy("t2", "o2") }
        }
}
