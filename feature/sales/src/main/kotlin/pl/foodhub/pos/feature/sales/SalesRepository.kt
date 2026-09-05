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
import pl.foodhub.pos.core.network.model.SalesDocumentDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
 * Online checkout against the DDD order/document endpoints, in the same order the web
 * POS runs them (foodhub-app sales/api/pos-runtime.ts): add lines to the order opened
 * when the table was occupied (TablesViewModel) -> finalize(paymentMethod) -> issue a
 * receipt, or an invoice when the buyer supplied a NIP (section 2.5 attribute picker
 * feeds attributeValueIds on either document).
 *
 * TODO(Faza 2): wrap this sequence in the offline write-ahead queue (core:sync) so a
 * connectivity drop mid-checkout does not lose the sale.
 */
class SalesRepository
    @Inject
    constructor(
        private val salesApi: SalesApi,
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
        ): ApiResult<String> =
            withContext(dispatchers.io) {
                addLines(orderId, lines)?.let { return@withContext it }

                val confirmed = apiCall { salesApi.confirm(orderId) }
                if (confirmed is ApiResult.HttpError) return@withContext confirmed
                if (confirmed is ApiResult.NetworkError) return@withContext confirmed

                val finalized =
                    apiCall { salesApi.finalize(orderId, FinalizeOrderRequestDto(options.paymentMethod.apiValue)) }
                if (finalized is ApiResult.HttpError) return@withContext finalized
                if (finalized is ApiResult.NetworkError) return@withContext finalized

                issueDocument(orderId, placeId, lines, options).map { it.id }
            }

        /** Adds every cart line to the draft order; returns the first failure, or null once all succeed. */
        private suspend fun addLines(
            orderId: String,
            lines: List<CartLine>,
        ): ApiResult<String>? {
            lines.forEach { line ->
                val added =
                    apiCall {
                        salesApi.addLine(
                            orderId = orderId,
                            body =
                                OrderLineRequestDto(
                                    productId = line.productId,
                                    productName = line.productName,
                                    quantity = line.quantity,
                                    unitPriceAmount = line.unitPriceGross.minorUnits,
                                ),
                        )
                    }
                if (added is ApiResult.HttpError) return added
                if (added is ApiResult.NetworkError) return added
            }
            return null
        }

        private suspend fun issueDocument(
            orderId: String,
            placeId: String,
            lines: List<CartLine>,
            options: CheckoutOptions,
        ): ApiResult<SalesDocumentDto> {
            val (paymentMethod, invoiceDetails, attributeValueIds) = options
            val documentLines = lines.toDocumentLines()
            val totalGrossAmount = lines.total().minorUnits

            return if (invoiceDetails != null) {
                apiCall {
                    salesApi.issueInvoice(
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
                        ),
                    )
                }
            } else {
                apiCall {
                    salesApi.issueReceipt(
                        IssueReceiptRequestDto(
                            orderId = orderId,
                            placeId = placeId,
                            lines = documentLines,
                            totalGrossAmount = totalGrossAmount,
                            paymentMethod = paymentMethod.apiValue,
                            attributeValueIds = attributeValueIds,
                        ),
                    )
                }
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
