package pl.foodhub.pos.feature.sales

import kotlinx.coroutines.withContext
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.common.map
import pl.foodhub.pos.core.network.api.SalesApi
import pl.foodhub.pos.core.network.apiCall
import pl.foodhub.pos.core.network.model.DocumentLineDto
import pl.foodhub.pos.core.network.model.FinalizeOrderRequestDto
import pl.foodhub.pos.core.network.model.IssueInvoiceRequestDto
import pl.foodhub.pos.core.network.model.IssueReceiptRequestDto
import pl.foodhub.pos.core.network.model.OrderLineRequestDto
import pl.foodhub.pos.core.sync.SyncQueue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

enum class PaymentMethod(val apiValue: String) {
    CASH("cash"),
    CARD("card"),
    OTHER("bank_transfer"),
}

data class SalesAttributeValue(val id: Int, val name: String)

data class SalesAttribute(val id: Int, val name: String, val values: List<SalesAttributeValue>)

/** Buyer details for an on-the-spot VAT invoice instead of a receipt (NIP required). */
data class InvoiceDetails(val buyerName: String, val buyerNip: String)

/** How to finalize a sale: payment, receipt vs. invoice, and the section 2.5 attribute picker. */
data class CheckoutOptions(
    val paymentMethod: PaymentMethod,
    val invoiceDetails: InvoiceDetails?,
    val attributeValueIds: List<Int>,
)

/**
 * Queues the checkout sequence -- add lines, confirm, finalize, then issue a receipt or
 * an invoice when the buyer supplied a NIP -- through [SyncQueue] instead of calling
 * the network directly, so a connectivity drop mid-checkout never loses the sale
 * (ANDROID_POS_ARCHITECTURE.md section 9 point 2, closing this class's former Faza 2
 * TODO). Every step's id (line/receipt/invoice) is generated here so a queued retry
 * after a dropped response is a backend no-op rather than a duplicate (section 9
 * point 4).
 */
class SalesRepository
    @Inject
    constructor(
        private val salesApi: SalesApi,
        private val syncQueue: SyncQueue,
        private val dispatchers: DispatcherProvider,
    ) {
        suspend fun attributes(): ApiResult<List<SalesAttribute>> =
            withContext(dispatchers.io) {
                apiCall { salesApi.salesAttributes() }.map { dtos ->
                    dtos.map { dto ->
                        SalesAttribute(
                            id = dto.id,
                            name = dto.name,
                            values = dto.values.map { SalesAttributeValue(it.id, it.name) },
                        )
                    }
                }
            }

        suspend fun checkout(
            orderId: String,
            placeId: String,
            lines: List<CartLine>,
            options: CheckoutOptions,
        ) = withContext(dispatchers.io) {
            lines.forEach { line ->
                syncQueue.addOrderLine(
                    orderId,
                    OrderLineRequestDto(
                        productId = line.productId,
                        productName = line.productName,
                        quantity = line.quantity,
                        unitPriceAmount = line.unitPriceGross.minorUnits,
                        lineId = UUID.randomUUID().toString(),
                    ),
                )
            }
            syncQueue.confirmOrder(orderId)
            syncQueue.finalizeOrder(orderId, FinalizeOrderRequestDto(options.paymentMethod.apiValue))
            issueDocument(orderId, placeId, lines, options)
        }

        private suspend fun issueDocument(
            orderId: String,
            placeId: String,
            lines: List<CartLine>,
            options: CheckoutOptions,
        ) {
            val (paymentMethod, invoiceDetails, attributeValueIds) = options
            val documentLines = lines.toDocumentLines()
            val totalGrossAmount = lines.total().minorUnits

            if (invoiceDetails != null) {
                syncQueue.issueInvoice(
                    IssueInvoiceRequestDto(
                        orderId = orderId,
                        placeId = placeId,
                        buyerName = invoiceDetails.buyerName,
                        buyerNip = invoiceDetails.buyerNip,
                        lines = documentLines,
                        totalGrossAmount = totalGrossAmount,
                        paymentMethod = paymentMethod.apiValue,
                        dueDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        attributeValueIds = attributeValueIds,
                        invoiceId = UUID.randomUUID().toString(),
                    ),
                )
            } else {
                syncQueue.issueReceipt(
                    IssueReceiptRequestDto(
                        orderId = orderId,
                        placeId = placeId,
                        lines = documentLines,
                        totalGrossAmount = totalGrossAmount,
                        paymentMethod = paymentMethod.apiValue,
                        attributeValueIds = attributeValueIds,
                        receiptId = UUID.randomUUID().toString(),
                    ),
                )
            }
        }

        private fun List<CartLine>.toDocumentLines(): List<DocumentLineDto> =
            map {
                DocumentLineDto(
                    lineId = it.productId,
                    productId = it.productId,
                    productName = it.productName,
                    quantity = it.quantity,
                    unitPriceAmount = it.unitPriceGross.minorUnits,
                )
            }
    }
