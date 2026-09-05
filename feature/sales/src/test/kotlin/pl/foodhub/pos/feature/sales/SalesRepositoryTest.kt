package pl.foodhub.pos.feature.sales

import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.common.Money
import pl.foodhub.pos.core.network.api.SalesApi
import pl.foodhub.pos.core.network.model.FinalizeOrderRequestDto
import pl.foodhub.pos.core.network.model.OrderLineRequestDto
import pl.foodhub.pos.core.sync.SyncQueue

@OptIn(ExperimentalCoroutinesApi::class)
private class TestDispatcherProvider(dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()) :
    DispatcherProvider {
    override val io = dispatcher
    override val default = dispatcher
    override val main = dispatcher
}

class SalesRepositoryTest {
    private val salesApi = mockk<SalesApi>()
    private val syncQueue = mockk<SyncQueue>(relaxed = true)
    private val repository = SalesRepository(salesApi, syncQueue, TestDispatcherProvider())

    private val lines =
        listOf(CartLine(productId = "p1", productName = "Pizza", unitPriceGross = Money(2500), quantity = 2))

    @Test
    fun `queues a receipt when no invoice details are supplied`() =
        runTest {
            repository.checkout(
                orderId = "o1",
                placeId = "place-1",
                lines = lines,
                options = CheckoutOptions(PaymentMethod.CASH, invoiceDetails = null, attributeValueIds = listOf(3)),
            )

            coVerify(exactly = 0) { syncQueue.issueInvoice(any()) }
            coVerify {
                syncQueue.addOrderLine(
                    "o1",
                    withArg { request ->
                        assertEquals("p1", request.productId)
                        assertEquals("Pizza", request.productName)
                        assertEquals(2, request.quantity)
                        assertEquals(2500, request.unitPriceAmount)
                        assertFalse(request.lineId.isNullOrBlank())
                    },
                )
            }
            coVerify { syncQueue.issueReceipt(withArg { assertEquals(listOf(3), it.attributeValueIds) }) }
            // Regression: the order must be confirmed (draft -> confirmed) before finalize is
            // queued, or the backend rejects finalize with a draft-order 422 once synced.
            coVerifyOrder {
                syncQueue.addOrderLine(any(), any())
                syncQueue.confirmOrder("o1")
                syncQueue.finalizeOrder("o1", FinalizeOrderRequestDto("cash"))
                syncQueue.issueReceipt(any())
            }
        }

    @Test
    fun `queues an invoice with the buyer NIP when invoice details are supplied`() =
        runTest {
            repository.checkout(
                orderId = "o1",
                placeId = "place-1",
                lines = lines,
                options =
                    CheckoutOptions(
                        paymentMethod = PaymentMethod.CASH,
                        invoiceDetails = InvoiceDetails(buyerName = "Jan Kowalski", buyerNip = "1234567890"),
                        attributeValueIds = emptyList(),
                    ),
            )

            coVerify(exactly = 0) { syncQueue.issueReceipt(any()) }
            coVerify {
                syncQueue.issueInvoice(
                    withArg { request ->
                        assertEquals("Jan Kowalski", request.buyerName)
                        assertEquals("1234567890", request.buyerNip)
                        assertTrue(request.dueDate.isNotBlank())
                        assertFalse(request.invoiceId.isNullOrBlank())
                    },
                )
            }
        }

    @Test
    fun `each cart line gets its own generated lineId`() =
        runTest {
            val twoLines =
                listOf(
                    CartLine(productId = "p1", productName = "Pizza", unitPriceGross = Money(2500), quantity = 1),
                    CartLine(productId = "p2", productName = "Cola", unitPriceGross = Money(500), quantity = 1),
                )
            val noInvoice = CheckoutOptions(PaymentMethod.CASH, invoiceDetails = null, attributeValueIds = emptyList())

            repository.checkout(orderId = "o1", placeId = "place-1", lines = twoLines, options = noInvoice)

            val captured = mutableListOf<OrderLineRequestDto>()
            coVerify(exactly = 2) { syncQueue.addOrderLine("o1", capture(captured)) }
            assertEquals(2, captured.mapNotNull { it.lineId }.toSet().size)
        }
}
