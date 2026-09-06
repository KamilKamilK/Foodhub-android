package pl.foodhub.pos.core.fiscal.novitus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.fiscal.FakeFiscalDeviceServer
import pl.foodhub.pos.core.fiscal.model.FiscalCommitResult
import pl.foodhub.pos.core.fiscal.model.FiscalLine
import pl.foodhub.pos.core.fiscal.model.FiscalPayment
import pl.foodhub.pos.core.fiscal.model.FiscalReceiptRequest
import pl.foodhub.pos.core.fiscal.transport.FiscalTcpConnectionFactory

private val FRAME_ACK = byteArrayOf(0x5C)
private val OK_STATUS = "1100001".toByteArray(Charsets.US_ASCII)

private class TestDispatchers : DispatcherProvider {
    override val io = Dispatchers.Unconfined
    override val default = Dispatchers.Unconfined
    override val main = Dispatchers.Unconfined
}

/**
 * Exercises [NovitusFiscalDriver] against a real (JVM-local) TCP listener --
 * [FakeFiscalDeviceServer] -- so the framed-read state machine in
 * [pl.foodhub.pos.core.fiscal.transport.SocketFiscalTcpConnection] runs for real,
 * not just the pure encode/decode logic already covered by
 * [NovitusFrameCodecTest]/[NovitusCrcTest].
 */
class NovitusFiscalDriverIntegrationTest {
    private val driver = NovitusFiscalDriver(FiscalTcpConnectionFactory(TestDispatchers()))

    private val request =
        FiscalReceiptRequest(
            lines =
                listOf(
                    FiscalLine(
                        name = "Pizza",
                        quantity = 1,
                        vatRateIndex = 0,
                        unitPriceGrosz = 2500,
                        totalGrosz = 2500,
                    ),
                ),
            payments = listOf(FiscalPayment(methodCode = "cash", amountGrosz = 2500)),
        )

    @Test
    fun `a full fiscalization round trip succeeds and reports the device's document number`() =
        runTest {
            val commitResponse = "000123".toByteArray(Charsets.US_ASCII) + byteArrayOf(0x5C)
            // cancel, begin, 1 line, 1 payment, commit, status poll, open drawer.
            val responses = listOf(FRAME_ACK, FRAME_ACK, FRAME_ACK, FRAME_ACK, commitResponse, OK_STATUS, FRAME_ACK)

            FakeFiscalDeviceServer(responses).use { server ->
                val result = driver.fiscalizeReceipt("127.0.0.1", server.port, request)

                assertTrue(result is FiscalCommitResult.Success)
                assertEquals("000123", (result as FiscalCommitResult.Success).fiscalDocumentNumber)
            }
        }

    @Test
    fun `an ambiguous status poll response is never reported as success`() =
        runTest {
            val commitResponse = "000123".toByteArray(Charsets.US_ASCII) + byteArrayOf(0x5C)
            val ambiguousStatus = "??????".toByteArray(Charsets.US_ASCII) + byteArrayOf(0x00)
            val errorDetail = FRAME_ACK
            // cancel, begin, 1 line, 1 payment, commit, ambiguous poll, then a queried #n detail.
            val responses =
                listOf(FRAME_ACK, FRAME_ACK, FRAME_ACK, FRAME_ACK, commitResponse, ambiguousStatus, errorDetail)

            FakeFiscalDeviceServer(responses).use { server ->
                val result = driver.fiscalizeReceipt("127.0.0.1", server.port, request)

                assertTrue(result is FiscalCommitResult.Ambiguous)
            }
        }

    @Test
    fun `an unreachable device is reported as unreachable, not a silent failure`() =
        runTest {
            val result = driver.fiscalizeReceipt("127.0.0.1", 1, request)

            assertEquals(FiscalCommitResult.Unreachable, result)
        }
}
