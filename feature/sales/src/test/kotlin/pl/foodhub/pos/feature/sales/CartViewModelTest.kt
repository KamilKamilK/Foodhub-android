package pl.foodhub.pos.feature.sales

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.foodhub.pos.core.auth.AuthRepository
import pl.foodhub.pos.core.auth.PosSession
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.common.Money
import pl.foodhub.pos.core.database.MenuCacheDao
import pl.foodhub.pos.core.sync.SyncQueue

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {
    private val salesRepository = mockk<SalesRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>()
    private val syncQueue = mockk<SyncQueue>(relaxed = true)
    private val menuCacheDao = mockk<MenuCacheDao>()

    private val product = PickerProduct(productId = "p1", name = "Pizza", unitPriceGross = Money(2500))

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { menuCacheDao.observeItems() } returns emptyFlow()
        every { authRepository.posSession } returns
            flowOf(PosSession(placeId = "place-1", placeName = "Bistro", posId = "pos-9"))
        coEvery { salesRepository.attributes() } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): CartViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("orderId" to "o1", "tableId" to "t1"))
        return CartViewModel(savedStateHandle, salesRepository, authRepository, syncQueue, menuCacheDao)
    }

    @Test
    fun `checkout is blocked when the cart is empty`() {
        val viewModel = viewModel()
        assertFalse(viewModel.state.value.canCheckout)
    }

    @Test
    fun `invoice toggle requires a buyer name and a valid NIP`() {
        val viewModel = viewModel()
        viewModel.addProduct(product)
        viewModel.setInvoiceRequested(true)

        assertFalse(viewModel.state.value.canCheckout)

        viewModel.onBuyerNameChange("Jan Kowalski")
        viewModel.onBuyerNipChange("123")
        assertFalse(viewModel.state.value.canCheckout)

        viewModel.onBuyerNipChange("1234567890")
        assertTrue(viewModel.state.value.canCheckout)
    }

    @Test
    fun `checkout queues the sale and releases the table`() =
        runTest {
            val viewModel = viewModel()
            viewModel.addProduct(product)

            viewModel.checkout()
            runCurrent()

            assertTrue(viewModel.state.value.queuedForSync)
            val expectedOptions =
                CheckoutOptions(PaymentMethod.CASH, invoiceDetails = null, attributeValueIds = emptyList())
            coVerify {
                salesRepository.checkout(
                    orderId = eq("o1"),
                    placeId = eq("place-1"),
                    lines = any(),
                    options = eq(expectedOptions),
                )
            }
            coVerify { syncQueue.releaseTable("t1", "o1") }
        }

    @Test
    fun `checkout surfaces an error when no place is resolved for the session`() =
        runTest {
            every { authRepository.posSession } returns flowOf(null)
            val viewModel = viewModel()
            viewModel.addProduct(product)

            viewModel.checkout()
            runCurrent()

            assertTrue(viewModel.state.value.error)
            coVerify(exactly = 0) { salesRepository.checkout(any(), any(), any(), any()) }
        }
}
