package pl.foodhub.pos.core.sync

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.database.TransactionQueue
import pl.foodhub.pos.core.network.api.SalesApi
import pl.foodhub.pos.core.network.api.TablesApi
import pl.foodhub.pos.core.network.model.FinalizeOrderRequestDto
import pl.foodhub.pos.core.network.model.OccupyTableResponseDto
import pl.foodhub.pos.core.network.model.OrderDto
import pl.foodhub.pos.core.printing.PrintRouter
import pl.foodhub.pos.core.printing.PrintableLine
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
private class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
) : DispatcherProvider {
    override val io = dispatcher
    override val default = dispatcher
    override val main = dispatcher
}

private fun httpException(status: Int): HttpException =
    HttpException(Response.error<Any>(status, "{}".toResponseBody("application/json".toMediaType())))

private val json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

class SyncProcessorTest {
    private val queue = mockk<TransactionQueue>()
    private val salesApi = mockk<SalesApi>()
    private val tablesApi = mockk<TablesApi>()
    private val printRouter = mockk<PrintRouter>()
    private val processor = SyncProcessor(queue, salesApi, tablesApi, printRouter, json, TestDispatcherProvider())

    private fun op(
        id: Long,
        type: SyncOperationType,
        payload: String,
    ) = TransactionQueue.PendingOperation(id, type.name, payload)

    @Test
    fun `drains every pending operation in order when each succeeds`() =
        runTest {
            val op1 = op(1, SyncOperationType.CONFIRM_ORDER, json.encodeToString(ConfirmOrderPayload("o1")))
            val op2 = op(2, SyncOperationType.RELEASE_TABLE, json.encodeToString(ReleaseTablePayload("t1", "o1")))
            coEvery { queue.nextPending() } returnsMany listOf(op1, op2, null)
            coEvery { salesApi.confirm("o1") } returns OrderDto(id = "o1")
            coEvery { tablesApi.release("t1", "o1") } returns Unit
            coEvery { queue.markSynced(any()) } returns Unit

            val result = processor.run()

            assertEquals(SyncRunResult.Drained, result)
            coVerify { queue.markSynced(1) }
            coVerify { queue.markSynced(2) }
        }

    @Test
    fun `stops the run without touching later operations on a network error`() =
        runTest {
            val op1 = op(1, SyncOperationType.CONFIRM_ORDER, json.encodeToString(ConfirmOrderPayload("o1")))
            coEvery { queue.nextPending() } returns op1
            coEvery { salesApi.confirm("o1") } throws IOException("offline")

            val result = processor.run()

            assertEquals(SyncRunResult.RetryLater, result)
            coVerify(exactly = 0) { queue.markSynced(any()) }
            coVerify(exactly = 0) { queue.markFailed(any(), any()) }
        }

    @Test
    fun `stops the run on a 5xx the same way as a network error`() =
        runTest {
            val op1 = op(1, SyncOperationType.CONFIRM_ORDER, json.encodeToString(ConfirmOrderPayload("o1")))
            coEvery { queue.nextPending() } returns op1
            coEvery { salesApi.confirm("o1") } throws httpException(503)

            val result = processor.run()

            assertEquals(SyncRunResult.RetryLater, result)
            coVerify(exactly = 0) { queue.markFailed(any(), any()) }
        }

    @Test
    fun `a 4xx on create-order is a genuine failure, not a retry artifact`() =
        runTest {
            val op1 = op(1, SyncOperationType.CREATE_ORDER, """{"placeId":"place-1","orderId":"o1"}""")
            coEvery { queue.nextPending() } returnsMany listOf(op1, null)
            coEvery { salesApi.createOrder(any()) } throws httpException(422)
            coEvery { queue.markFailed(1, any()) } returns Unit

            val result = processor.run()

            assertEquals(SyncRunResult.Drained, result)
            coVerify { queue.markFailed(1, any()) }
        }

    @Test
    fun `a 422 on confirm-order already past draft is reconciled as synced`() =
        runTest {
            val op1 = op(1, SyncOperationType.CONFIRM_ORDER, json.encodeToString(ConfirmOrderPayload("o1")))
            coEvery { queue.nextPending() } returnsMany listOf(op1, null)
            coEvery { salesApi.confirm("o1") } throws httpException(422)
            coEvery { salesApi.getOrder("o1") } returns OrderDto(id = "o1", status = "confirmed")
            coEvery { queue.markSynced(1) } returns Unit

            val result = processor.run()

            assertEquals(SyncRunResult.Drained, result)
            coVerify { queue.markSynced(1) }
            coVerify(exactly = 0) { queue.markFailed(any(), any()) }
        }

    @Test
    fun `a 422 on confirm-order still in draft is a genuine conflict`() =
        runTest {
            val op1 = op(1, SyncOperationType.CONFIRM_ORDER, json.encodeToString(ConfirmOrderPayload("o1")))
            coEvery { queue.nextPending() } returnsMany listOf(op1, null)
            coEvery { salesApi.confirm("o1") } throws httpException(422)
            coEvery { salesApi.getOrder("o1") } returns OrderDto(id = "o1", status = "draft")
            coEvery { queue.markFailed(1, any()) } returns Unit

            val result = processor.run()

            assertEquals(SyncRunResult.Drained, result)
            coVerify { queue.markFailed(1, any()) }
            coVerify(exactly = 0) { queue.markSynced(any()) }
        }

    @Test
    fun `a 422 on finalize-order already completed is reconciled as synced`() =
        runTest {
            val payload = json.encodeToString(FinalizeOrderPayload("o1", FinalizeOrderRequestDto("cash")))
            val op1 = op(1, SyncOperationType.FINALIZE_ORDER, payload)
            coEvery { queue.nextPending() } returnsMany listOf(op1, null)
            coEvery { salesApi.finalize("o1", any()) } throws httpException(422)
            coEvery { salesApi.getOrder("o1") } returns OrderDto(id = "o1", status = "completed")
            coEvery { queue.markSynced(1) } returns Unit

            val result = processor.run()

            assertEquals(SyncRunResult.Drained, result)
            coVerify { queue.markSynced(1) }
        }

    @Test
    fun `occupy conflict is still a synced no-op, not a failure`() =
        runTest {
            val op1 = op(1, SyncOperationType.OCCUPY_TABLE, json.encodeToString(OccupyTablePayload("t1", "o1")))
            coEvery { queue.nextPending() } returnsMany listOf(op1, null)
            coEvery { tablesApi.occupy("t1", "o1") } returns OccupyTableResponseDto("t1", "o2", conflict = true)
            coEvery { queue.markSynced(1) } returns Unit

            val result = processor.run()

            assertEquals(SyncRunResult.Drained, result)
            coVerify { queue.markSynced(1) }
            coVerify(exactly = 0) { queue.markFailed(any(), any()) }
        }

    @Test
    fun `print-kitchen-tickets dispatches through PrintRouter and marks synced on success`() =
        runTest {
            val lines = listOf(PrintableLine("Pizza", 2, orderDirectionId = 1L, unitPriceAmount = 2500))
            val payload = json.encodeToString(PrintKitchenTicketsPayload("place-1", "o1", lines))
            val op1 = op(1, SyncOperationType.PRINT_KITCHEN_TICKETS, payload)
            coEvery { queue.nextPending() } returnsMany listOf(op1, null)
            coEvery { printRouter.printKitchenTickets("place-1", "o1", lines) } returns ApiResult.Success(Unit)
            coEvery { queue.markSynced(1) } returns Unit

            val result = processor.run()

            assertEquals(SyncRunResult.Drained, result)
            coVerify { queue.markSynced(1) }
        }

    @Test
    fun `a failed print does not retry the whole queue -- it is marked failed and moved past`() =
        runTest {
            val lines = listOf(PrintableLine("Pizza", 2, orderDirectionId = 1L, unitPriceAmount = 2500))
            val payload = json.encodeToString(PrintReceiptPayload("place-1", "o1", lines, 5000, "cash"))
            val op1 = op(1, SyncOperationType.PRINT_RECEIPT, payload)
            coEvery { queue.nextPending() } returnsMany listOf(op1, null)
            coEvery { printRouter.printReceipt("place-1", "o1", lines, 5000, "cash") } returns
                ApiResult.HttpError(400, "PRINT_FAILED", null)
            coEvery { queue.markFailed(1, any()) } returns Unit

            val result = processor.run()

            assertEquals(SyncRunResult.Drained, result)
            coVerify { queue.markFailed(1, any()) }
            coVerify(exactly = 0) { queue.markSynced(any()) }
        }

    @Test
    fun `an unknown operation type is marked failed and does not block the queue`() =
        runTest {
            val op1 = op(1, SyncOperationType.CONFIRM_ORDER, "irrelevant").copy(type = "SOMETHING_REMOVED")
            coEvery { queue.nextPending() } returnsMany listOf(op1, null)
            coEvery { queue.markFailed(1, any()) } returns Unit

            val result = processor.run()

            assertEquals(SyncRunResult.Drained, result)
            coVerify { queue.markFailed(1, any()) }
        }
}
