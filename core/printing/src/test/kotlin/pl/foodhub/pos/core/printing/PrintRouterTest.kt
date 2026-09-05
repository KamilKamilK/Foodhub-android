package pl.foodhub.pos.core.printing

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.network.api.PrintersApi
import pl.foodhub.pos.core.network.model.PrinterDto
import java.io.IOException

class PrintRouterTest {
    private val printersApi = mockk<PrintersApi>()
    private val printerClient = mockk<LanPrinterClient>()
    private val router = PrintRouter(printersApi, printerClient)

    private val kitchenPrinter =
        PrinterDto(
            id = "p1",
            name = "Kuchnia",
            ip = "10.0.0.1",
            port = "9100",
            role = "KITCHEN",
            orderDirectionIds = listOf(1L),
        )
    private val barPrinter =
        PrinterDto(
            id = "p2",
            name = "Bar",
            ip = "10.0.0.2",
            port = "9100",
            role = "KITCHEN",
            orderDirectionIds = listOf(2L),
        )
    private val receiptPrinter =
        PrinterDto(
            id = "p3",
            name = "Paragon",
            ip = "10.0.0.3",
            port = "9100",
            role = "RECEIPT",
            orderDirectionIds = emptyList(),
        )

    private val kitchenLine = PrintableLine("Pizza", 1, orderDirectionId = 1L, unitPriceAmount = 2500)
    private val barLine = PrintableLine("Cola", 1, orderDirectionId = 2L, unitPriceAmount = 500)

    @Test
    fun `sends only the lines matching a kitchen printer's directions`() =
        runTest {
            coEvery { printersApi.printers("place-1") } returns listOf(kitchenPrinter, barPrinter)
            coEvery { printerClient.send(any(), any(), any()) } returns Result.success(Unit)

            val result = router.printKitchenTickets("place-1", "order-1", listOf(kitchenLine, barLine))

            assertEquals(ApiResult.Success(Unit), result)
            coVerify(exactly = 1) { printerClient.send("10.0.0.1", 9100, any()) }
            coVerify(exactly = 1) { printerClient.send("10.0.0.2", 9100, any()) }
        }

    @Test
    fun `skips a kitchen printer with no matching lines without failing`() =
        runTest {
            coEvery { printersApi.printers("place-1") } returns listOf(kitchenPrinter)

            val result = router.printKitchenTickets("place-1", "order-1", listOf(barLine))

            assertEquals(ApiResult.Success(Unit), result)
            coVerify(exactly = 0) { printerClient.send(any(), any(), any()) }
        }

    @Test
    fun `sends the full receipt to every RECEIPT-role printer regardless of direction`() =
        runTest {
            coEvery { printersApi.printers("place-1") } returns listOf(kitchenPrinter, receiptPrinter)
            coEvery { printerClient.send(any(), any(), any()) } returns Result.success(Unit)

            val result = router.printReceipt("place-1", "order-1", listOf(kitchenLine, barLine), 3000, "cash")

            assertEquals(ApiResult.Success(Unit), result)
            coVerify(exactly = 1) { printerClient.send("10.0.0.3", 9100, any()) }
            coVerify(exactly = 0) { printerClient.send("10.0.0.1", any(), any()) }
        }

    @Test
    fun `is a trivial success when the place has no printer of the requested role`() =
        runTest {
            coEvery { printersApi.printers("place-1") } returns emptyList()

            val result = router.printReceipt("place-1", "order-1", listOf(kitchenLine), 2500, "cash")

            assertEquals(ApiResult.Success(Unit), result)
        }

    @Test
    fun `reports a non-5xx HttpError when the printer list can't be fetched`() =
        runTest {
            coEvery { printersApi.printers("place-1") } throws IOException("offline")

            val result = router.printReceipt("place-1", "order-1", listOf(kitchenLine), 2500, "cash")

            assertTrue(result is ApiResult.HttpError)
            assertTrue((result as ApiResult.HttpError).status < 500)
        }

    @Test
    fun `a socket failure on one printer is reported but does not stop the other printers`() =
        runTest {
            coEvery { printersApi.printers("place-1") } returns listOf(kitchenPrinter, barPrinter)
            coEvery { printerClient.send("10.0.0.1", 9100, any()) } returns Result.failure(IOException("unreachable"))
            coEvery { printerClient.send("10.0.0.2", 9100, any()) } returns Result.success(Unit)

            val result = router.printKitchenTickets("place-1", "order-1", listOf(kitchenLine, barLine))

            assertTrue(result is ApiResult.HttpError)
            assertTrue((result as ApiResult.HttpError).status < 500)
            coVerify { printerClient.send("10.0.0.2", 9100, any()) }
        }

    @Test
    fun `a printer with a non-numeric port is skipped as failed rather than crashing`() =
        runTest {
            val brokenPrinter = receiptPrinter.copy(port = "not-a-port")
            coEvery { printersApi.printers("place-1") } returns listOf(brokenPrinter)

            val result = router.printReceipt("place-1", "order-1", listOf(kitchenLine), 2500, "cash")

            assertTrue(result is ApiResult.HttpError)
            coVerify(exactly = 0) { printerClient.send(any(), any(), any()) }
        }
}
