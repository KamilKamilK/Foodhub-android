package pl.foodhub.pos.core.sync

import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.database.TransactionQueue
import pl.foodhub.pos.core.network.api.SalesApi
import pl.foodhub.pos.core.network.api.TablesApi
import pl.foodhub.pos.core.network.apiCall
import pl.foodhub.pos.core.network.model.CreateOrderRequestDto
import pl.foodhub.pos.core.network.model.IssueInvoiceRequestDto
import pl.foodhub.pos.core.network.model.IssueReceiptRequestDto
import javax.inject.Inject

private const val HTTP_UNPROCESSABLE_ENTITY = 422
private const val HTTP_SERVER_ERROR_RANGE_START = 500
private const val HTTP_SERVER_ERROR_RANGE_END = 599

sealed interface SyncRunResult {
    data object Drained : SyncRunResult

    data object RetryLater : SyncRunResult
}

/**
 * Drains the offline queue FIFO, one operation at a time, in the order they were
 * enqueued -- ordering matters because a checkout is a dependent sequence (add lines ->
 * confirm -> finalize -> issue document). A transient failure (no connectivity, or a
 * 5xx) stops the run entirely rather than skipping ahead, so a later operation is never
 * attempted before an earlier one it depends on. Framework-free on purpose: this is
 * plain, directly unit-testable logic, with [SyncWorker] as its only (thin) caller.
 */
class SyncProcessor
    @Inject
    constructor(
        private val queue: TransactionQueue,
        private val salesApi: SalesApi,
        private val tablesApi: TablesApi,
        private val json: Json,
        private val dispatchers: DispatcherProvider,
    ) {
        suspend fun run(): SyncRunResult = withContext(dispatchers.io) { drain() }

        private suspend fun drain(): SyncRunResult {
            while (true) {
                val operation = queue.nextPending() ?: return SyncRunResult.Drained
                if (processOne(operation) == StepOutcome.RETRY_LATER) return SyncRunResult.RetryLater
            }
        }

        private enum class StepOutcome { CONTINUE, RETRY_LATER }

        private suspend fun processOne(operation: TransactionQueue.PendingOperation): StepOutcome {
            val type = SyncOperationType.entries.find { it.name == operation.type }
            if (type == null) {
                queue.markFailed(operation.id, "Unknown operation type: ${operation.type}")
                return StepOutcome.CONTINUE
            }

            return when (val result = dispatch(type, operation.payloadJson)) {
                is ApiResult.Success -> {
                    queue.markSynced(operation.id)
                    StepOutcome.CONTINUE
                }
                is ApiResult.NetworkError -> StepOutcome.RETRY_LATER
                is ApiResult.HttpError -> handleHttpError(operation, type, result)
            }
        }

        private suspend fun dispatch(
            type: SyncOperationType,
            payloadJson: String,
        ): ApiResult<*> =
            when (type) {
                SyncOperationType.OCCUPY_TABLE -> {
                    val p = json.decodeFromString<OccupyTablePayload>(payloadJson)
                    apiCall { tablesApi.occupy(p.tableId, p.orderId) }
                }
                SyncOperationType.RELEASE_TABLE -> {
                    val p = json.decodeFromString<ReleaseTablePayload>(payloadJson)
                    apiCall { tablesApi.release(p.tableId, p.orderId) }
                }
                SyncOperationType.CREATE_ORDER -> {
                    val p = json.decodeFromString<CreateOrderRequestDto>(payloadJson)
                    apiCall { salesApi.createOrder(p) }
                }
                SyncOperationType.ADD_ORDER_LINE -> {
                    val p = json.decodeFromString<AddOrderLinePayload>(payloadJson)
                    apiCall { salesApi.addLine(p.orderId, p.request) }
                }
                SyncOperationType.CONFIRM_ORDER -> {
                    val p = json.decodeFromString<ConfirmOrderPayload>(payloadJson)
                    apiCall { salesApi.confirm(p.orderId) }
                }
                SyncOperationType.FINALIZE_ORDER -> {
                    val p = json.decodeFromString<FinalizeOrderPayload>(payloadJson)
                    apiCall { salesApi.finalize(p.orderId, p.request) }
                }
                SyncOperationType.ISSUE_RECEIPT -> {
                    val p = json.decodeFromString<IssueReceiptRequestDto>(payloadJson)
                    apiCall { salesApi.issueReceipt(p) }
                }
                SyncOperationType.ISSUE_INVOICE -> {
                    val p = json.decodeFromString<IssueInvoiceRequestDto>(payloadJson)
                    apiCall { salesApi.issueInvoice(p) }
                }
            }

        /**
         * A 5xx (or no response at all) is transient -- retry the whole run later. A
         * 4xx is, after core:sync's operations were made idempotent-safe on the
         * backend, a genuine failure for every type except confirm/finalize: those two
         * can 422 on a harmless retry of an already-applied change (the documented
         * contract in ANDROID_POS_ARCHITECTURE.md section 9 point 4), so a 422 there
         * is reconciled against the order's actual current status before being treated
         * as a real failure.
         */
        private suspend fun handleHttpError(
            operation: TransactionQueue.PendingOperation,
            type: SyncOperationType,
            error: ApiResult.HttpError,
        ): StepOutcome {
            val isServerError = error.status in HTTP_SERVER_ERROR_RANGE_START..HTTP_SERVER_ERROR_RANGE_END
            if (isServerError) return StepOutcome.RETRY_LATER

            if (error.status == HTTP_UNPROCESSABLE_ENTITY) {
                val orderId = reconcilableOrderId(type, operation.payloadJson)
                if (orderId != null && alreadyApplied(type, orderId)) {
                    queue.markSynced(operation.id)
                    return StepOutcome.CONTINUE
                }
            }

            queue.markFailed(
                operation.id,
                "HTTP ${error.status}: ${error.message ?: error.errorCode ?: "unknown error"}",
            )
            return StepOutcome.CONTINUE
        }

        private fun reconcilableOrderId(
            type: SyncOperationType,
            payloadJson: String,
        ): String? =
            when (type) {
                SyncOperationType.CONFIRM_ORDER -> json.decodeFromString<ConfirmOrderPayload>(payloadJson).orderId
                SyncOperationType.FINALIZE_ORDER -> json.decodeFromString<FinalizeOrderPayload>(payloadJson).orderId
                else -> null
            }

        private suspend fun alreadyApplied(
            type: SyncOperationType,
            orderId: String,
        ): Boolean {
            val order = (apiCall { salesApi.getOrder(orderId) } as? ApiResult.Success)?.value ?: return false
            return when (type) {
                SyncOperationType.CONFIRM_ORDER -> order.status != "draft"
                SyncOperationType.FINALIZE_ORDER -> order.status == "completed"
                else -> false
            }
        }
    }
