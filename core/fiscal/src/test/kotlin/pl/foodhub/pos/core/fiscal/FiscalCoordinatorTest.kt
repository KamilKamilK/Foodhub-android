package pl.foodhub.pos.core.fiscal

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.foodhub.pos.core.database.FiscalizationLedger
import pl.foodhub.pos.core.database.FiscalizationRecordEntity
import pl.foodhub.pos.core.fiscal.model.FiscalCommitResult
import pl.foodhub.pos.core.fiscal.model.FiscalReceiptRequest
import pl.foodhub.pos.core.network.api.FiscalDeviceApi
import pl.foodhub.pos.core.network.model.FiscalDeviceDto
import retrofit2.HttpException
import retrofit2.Response

class FiscalCoordinatorTest {
    private val fiscalDeviceApi = mockk<FiscalDeviceApi>()
    private val driver = mockk<FiscalPrinterDriver>()
    private val driverFactory = mockk<FiscalDriverFactory> { every { forManufacturer(any()) } returns driver }
    private val ledger = mockk<FiscalizationLedger>(relaxed = true)
    private val coordinator = FiscalCoordinator(fiscalDeviceApi, driverFactory, ledger)

    private val device =
        FiscalDeviceDto(
            id = "device-1",
            placeId = "place-1",
            posId = "pos-1",
            manufacturer = "NOVITUS",
            ip = "10.0.0.1",
            port = "6666",
        )
    private val request = FiscalReceiptRequest(lines = emptyList(), payments = emptyList())

    @Test
    fun `returns NotConfigured when the pos has no fiscal device`() =
        runTest {
            coEvery { ledger.findByDocumentId("receipt-1") } returns null
            coEvery { fiscalDeviceApi.fiscalDevice("place-1", "pos-1") } throws notFound()

            val result = coordinator.fiscalizeReceipt("place-1", "pos-1", "receipt-1", request)

            assertEquals(FiscalOutcome.NotConfigured, result)
            coVerify(exactly = 0) { driver.fiscalizeReceipt(any(), any(), any()) }
        }

    @Test
    fun `a ledger hit is returned without ever calling the driver -- never double-commits`() =
        runTest {
            coEvery { ledger.findByDocumentId("receipt-1") } returns
                FiscalizationRecordEntity("receipt-1", "RECEIPT", "device-1", "FISCAL/1", "DAILY/1", 0L)

            val result = coordinator.fiscalizeReceipt("place-1", "pos-1", "receipt-1", request)

            assertEquals(FiscalOutcome.Fiscalized("device-1", "FISCAL/1", "DAILY/1"), result)
            coVerify(exactly = 0) { fiscalDeviceApi.fiscalDevice(any(), any()) }
            coVerify(exactly = 0) { driver.fiscalizeReceipt(any(), any(), any()) }
        }

    @Test
    fun `a successful hardware commit is recorded to the ledger and reported as Fiscalized`() =
        runTest {
            coEvery { ledger.findByDocumentId("receipt-1") } returns null
            coEvery { fiscalDeviceApi.fiscalDevice("place-1", "pos-1") } returns device
            coEvery { driver.fiscalizeReceipt("10.0.0.1", 6666, request) } returns
                FiscalCommitResult.Success("FISCAL/1", "DAILY/1")

            val result = coordinator.fiscalizeReceipt("place-1", "pos-1", "receipt-1", request)

            assertEquals(FiscalOutcome.Fiscalized("device-1", "FISCAL/1", "DAILY/1"), result)
            coVerify {
                ledger.record(
                    withArg { record ->
                        assertEquals("receipt-1", record.documentId)
                        assertEquals("RECEIPT", record.documentType)
                        assertEquals("device-1", record.fiscalDeviceId)
                        assertEquals("FISCAL/1", record.fiscalDocumentNumber)
                        assertEquals("DAILY/1", record.dailyReportNumber)
                    },
                )
            }
        }

    @Test
    fun `a driver failure is never written to the ledger -- a retry can safely re-attempt the hardware step`() =
        runTest {
            coEvery { ledger.findByDocumentId("receipt-1") } returns null
            coEvery { fiscalDeviceApi.fiscalDevice("place-1", "pos-1") } returns device
            coEvery { driver.fiscalizeReceipt(any(), any(), any()) } returns FiscalCommitResult.Unreachable

            val result = coordinator.fiscalizeReceipt("place-1", "pos-1", "receipt-1", request)

            assertTrue(result is FiscalOutcome.Failed)
            coVerify(exactly = 0) { ledger.record(any()) }
        }

    private fun notFound(): HttpException =
        HttpException(Response.error<Any>(404, "{}".toResponseBody("application/json".toMediaType())))
}
