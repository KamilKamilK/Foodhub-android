package pl.foodhub.pos.core.fiscal.posnet

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

private val FRAME_ACK = byteArrayOf(0x03)

private class TestDispatchers : DispatcherProvider {
    override val io = Dispatchers.Unconfined
    override val default = Dispatchers.Unconfined
    override val main = Dispatchers.Unconfined
}

/**
 * Exercises [PosnetFiscalDriver] against a real (JVM-local) TCP listener --
 * [FakeFiscalDeviceServer] -- so the framed-read state machine runs for real, not
 * just the pure encode/decode logic already covered by [PosnetFrameCodecTest]/
 * [PosnetCrc16Test].
 */
class PosnetFiscalDriverIntegrationTest {
    private val driver = PosnetFiscalDriver(FiscalTcpConnectionFactory(TestDispatchers()))

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
            val endResponse = PosnetFrameCodec.buildFrame(listOf("bt00042"))
            // init, 1 line, 1 payment, footer, end, feed, drawer, cancel.
            val responses =
                listOf(FRAME_ACK, FRAME_ACK, FRAME_ACK, FRAME_ACK, endResponse, FRAME_ACK, FRAME_ACK, FRAME_ACK)

            FakeFiscalDeviceServer(responses).use { server ->
                val result = driver.fiscalizeReceipt("127.0.0.1", server.port, request)

                assertTrue(result is FiscalCommitResult.Success)
                assertEquals("00042", (result as FiscalCommitResult.Success).fiscalDocumentNumber)
            }
        }

    @Test
    fun `a device error frame on commit is reported as a device error, not success`() =
        runTest {
            val errorResponse = "?7#".toByteArray(Charsets.US_ASCII) + byteArrayOf(0x03)
            val responses = listOf(FRAME_ACK, FRAME_ACK, FRAME_ACK, FRAME_ACK, errorResponse)

            FakeFiscalDeviceServer(responses).use { server ->
                val result = driver.fiscalizeReceipt("127.0.0.1", server.port, request)

                assertTrue(result is FiscalCommitResult.DeviceError)
                assertEquals("7", (result as FiscalCommitResult.DeviceError).code)
            }
        }

    @Test
    fun `an unreachable device is reported as unreachable, not a silent failure`() =
        runTest {
            val result = driver.fiscalizeReceipt("127.0.0.1", 1, request)

            assertEquals(FiscalCommitResult.Unreachable, result)
        }
}
