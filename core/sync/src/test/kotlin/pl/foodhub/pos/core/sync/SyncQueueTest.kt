package pl.foodhub.pos.core.sync

import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.foodhub.pos.core.database.TransactionQueue
import pl.foodhub.pos.core.network.model.FinalizeOrderRequestDto
import pl.foodhub.pos.core.network.model.IssueReceiptRequestDto

class SyncQueueTest {
    private val transactionQueue = mockk<TransactionQueue>(relaxed = true)
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    private val syncQueue = SyncQueue(transactionQueue, scheduler, json)

    @Test
    fun `occupyTable enqueues the operation and schedules a sync`() =
        runTest {
            syncQueue.occupyTable("t1", "o1")

            coVerify {
                transactionQueue.enqueue(
                    SyncOperationType.OCCUPY_TABLE.name,
                    withArg { payload ->
                        val decoded = json.decodeFromString<OccupyTablePayload>(payload)
                        assertEquals("t1", decoded.tableId)
                        assertEquals("o1", decoded.orderId)
                    },
                )
            }
            verify { scheduler.scheduleSync() }
        }

    @Test
    fun `finalizeOrder encodes the order id and request together`() =
        runTest {
            syncQueue.finalizeOrder("o1", FinalizeOrderRequestDto("cash"))

            coVerify {
                transactionQueue.enqueue(
                    SyncOperationType.FINALIZE_ORDER.name,
                    withArg { payload ->
                        val decoded = json.decodeFromString<FinalizeOrderPayload>(payload)
                        assertEquals("o1", decoded.orderId)
                        assertEquals("cash", decoded.request.paymentMethod)
                    },
                )
            }
        }

    @Test
    fun `issueReceipt reuses IssueReceiptRequestDto directly as the payload`() =
        runTest {
            val request =
                IssueReceiptRequestDto(
                    orderId = "o1",
                    placeId = "place-1",
                    lines = emptyList(),
                    totalGrossAmount = 1000,
                    paymentMethod = "cash",
                    receiptId = "r1",
                )

            syncQueue.issueReceipt(request)

            coVerify {
                transactionQueue.enqueue(
                    SyncOperationType.ISSUE_RECEIPT.name,
                    withArg { payload ->
                        assertEquals(request, json.decodeFromString<IssueReceiptRequestDto>(payload))
                    },
                )
            }
        }
}
