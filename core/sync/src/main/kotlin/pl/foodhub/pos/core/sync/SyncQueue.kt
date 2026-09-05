package pl.foodhub.pos.core.sync

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.foodhub.pos.core.database.TransactionQueue
import pl.foodhub.pos.core.network.model.CreateOrderRequestDto
import pl.foodhub.pos.core.network.model.FinalizeOrderRequestDto
import pl.foodhub.pos.core.network.model.IssueInvoiceRequestDto
import pl.foodhub.pos.core.network.model.IssueReceiptRequestDto
import pl.foodhub.pos.core.network.model.OrderLineRequestDto
import pl.foodhub.pos.core.printing.PrintableLine
import javax.inject.Inject

/**
 * The only way `feature:*` code writes to the offline queue -- never touches
 * [TransactionQueue]/Room directly. Every call here returns immediately once the
 * operation is durably queued; the actual network call happens later, in
 * [SyncWorker], whenever connectivity allows (ANDROID_POS_ARCHITECTURE.md section 9
 * point 2: "UI pokazuje sukces natychmiast").
 *
 * One small delegating method per [SyncOperationType] by design -- this class is
 * deliberately the single write-path entry point for the whole queue, so splitting it
 * to dodge detekt's function-count threshold would fragment that guarantee.
 */
@Suppress("TooManyFunctions")
class SyncQueue
    @Inject
    constructor(
        private val transactionQueue: TransactionQueue,
        private val scheduler: SyncScheduler,
        private val json: Json,
    ) {
        suspend fun occupyTable(
            tableId: String,
            orderId: String,
        ) = enqueue(SyncOperationType.OCCUPY_TABLE, OccupyTablePayload(tableId, orderId))

        suspend fun releaseTable(
            tableId: String,
            orderId: String,
        ) = enqueue(SyncOperationType.RELEASE_TABLE, ReleaseTablePayload(tableId, orderId))

        suspend fun createOrder(
            orderId: String,
            placeId: String,
        ) = enqueue(SyncOperationType.CREATE_ORDER, CreateOrderRequestDto(placeId = placeId, orderId = orderId))

        suspend fun addOrderLine(
            orderId: String,
            request: OrderLineRequestDto,
        ) = enqueue(SyncOperationType.ADD_ORDER_LINE, AddOrderLinePayload(orderId, request))

        suspend fun confirmOrder(orderId: String) =
            enqueue(
                SyncOperationType.CONFIRM_ORDER,
                ConfirmOrderPayload(orderId),
            )

        suspend fun finalizeOrder(
            orderId: String,
            request: FinalizeOrderRequestDto,
        ) = enqueue(SyncOperationType.FINALIZE_ORDER, FinalizeOrderPayload(orderId, request))

        suspend fun issueReceipt(request: IssueReceiptRequestDto) = enqueue(SyncOperationType.ISSUE_RECEIPT, request)

        suspend fun issueInvoice(request: IssueInvoiceRequestDto) = enqueue(SyncOperationType.ISSUE_INVOICE, request)

        suspend fun printKitchenTickets(
            orderId: String,
            placeId: String,
            lines: List<PrintableLine>,
        ) = enqueue(SyncOperationType.PRINT_KITCHEN_TICKETS, PrintKitchenTicketsPayload(placeId, orderId, lines))

        suspend fun printReceipt(
            orderId: String,
            placeId: String,
            lines: List<PrintableLine>,
            totalGrossAmount: Long,
            paymentMethod: String,
        ) = enqueue(
            SyncOperationType.PRINT_RECEIPT,
            PrintReceiptPayload(placeId, orderId, lines, totalGrossAmount, paymentMethod),
        )

        private suspend inline fun <reified T> enqueue(
            type: SyncOperationType,
            payload: T,
        ) {
            transactionQueue.enqueue(type.name, json.encodeToString(payload))
            scheduler.scheduleSync()
        }
    }
