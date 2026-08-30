package pl.foodhub.pos.feature.sales

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.common.Money
import pl.foodhub.pos.core.network.api.SalesApi
import pl.foodhub.pos.core.network.model.FinalizeOrderRequestDto
import pl.foodhub.pos.core.network.model.OrderDto
import pl.foodhub.pos.core.network.model.OrderLineRequestDto
import pl.foodhub.pos.core.network.model.SalesDocumentDto
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
private class TestDispatcherProvider(dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()) :
    DispatcherProvider {
    override val io = dispatcher
    override val default = dispatcher
    override val main = dispatcher
}

class SalesRepositoryTest {
    private val salesApi = mockk<SalesApi>()
    private val repository = SalesRepository(salesApi, TestDispatcherProvider())

    private val lines =
        listOf(CartLine(productId = "p1", productName = "Pizza", unitPriceGross = Money(2500), quantity = 2))

    @Test
    fun `issues a receipt when no invoice details are supplied`() =
        runTest {
            coEvery { salesApi.addLine(any(), any()) } returns OrderDto(id = "o1")
            coEvery { salesApi.finalize("o1", FinalizeOrderRequestDto("cash")) } returns OrderDto(id = "o1")
            coEvery { salesApi.issueReceipt(any()) } returns SalesDocumentDto(id = "r1")

            val result =
                repository.checkout(
                    orderId = "o1",
                    placeId = "place-1",
                    lines = lines,
                    options = CheckoutOptions(PaymentMethod.CASH, invoiceDetails = null, attributeValueIds = listOf(3)),
                )

            assertEquals(ApiResult.Success("r1"), result)
            coVerify(exactly = 0) { salesApi.issueInvoice(any()) }
            coVerify {
                salesApi.addLine(
                    "o1",
                    OrderLineRequestDto(productId = "p1", productName = "Pizza", quantity = 2, unitPriceAmount = 2500),
                )
            }
        }

    @Test
    fun `issues an invoice with the buyer NIP when invoice details are supplied`() =
        runTest {
            coEvery { salesApi.addLine(any(), any()) } returns OrderDto(id = "o1")
            coEvery { salesApi.finalize(any(), any()) } returns OrderDto(id = "o1")
            coEvery { salesApi.issueInvoice(any()) } returns SalesDocumentDto(id = "i1")

            val result =
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

            assertEquals(ApiResult.Success("i1"), result)
            coVerify(exactly = 0) { salesApi.issueReceipt(any()) }
            coVerify {
                salesApi.issueInvoice(
                    withArg { request ->
                        assertEquals("Jan Kowalski", request.buyerName)
                        assertEquals("1234567890", request.buyerNip)
                        assertTrue(request.dueDate.isNotBlank())
                    },
                )
            }
        }

    @Test
    fun `stops before finalizing when adding a line fails`() =
        runTest {
            coEvery { salesApi.addLine(any(), any()) } throws IOException("offline")

            val noInvoice = CheckoutOptions(PaymentMethod.CASH, invoiceDetails = null, attributeValueIds = emptyList())
            val result =
                repository.checkout(orderId = "o1", placeId = "place-1", lines = lines, options = noInvoice)

            assertTrue(result is ApiResult.NetworkError)
            coVerify(exactly = 0) { salesApi.finalize(any(), any()) }
            coVerify(exactly = 0) { salesApi.issueReceipt(any()) }
        }
}
