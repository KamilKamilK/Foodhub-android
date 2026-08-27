package pl.foodhub.pos.feature.sales

import kotlinx.coroutines.withContext
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.network.api.SalesApi
import pl.foodhub.pos.core.network.apiCall
import pl.foodhub.pos.core.network.model.CreateOrderRequestDto
import pl.foodhub.pos.core.network.model.DocumentLineDto
import pl.foodhub.pos.core.network.model.FinalizeOrderRequestDto
import pl.foodhub.pos.core.network.model.IssueReceiptRequestDto
import pl.foodhub.pos.core.network.model.OrderLineRequestDto
import javax.inject.Inject

enum class PaymentMethod(val apiValue: String) {
    CASH("cash"),
    CARD("card"),
    OTHER("bank_transfer"),
}

/**
 * Online checkout against the DDD order/document endpoints, in the same order the web
 * POS runs them (foodhub-app sales/api/pos-runtime.ts):
 * create order -> add lines -> finalize(paymentMethod) -> issue receipt.
 *
 * TODO(Faza 2): wrap this sequence in the offline write-ahead queue (core:sync) so a
 * connectivity drop mid-checkout does not lose the sale.
 * TODO(Faza 1): invoice-on-NIP branch, sales-document attribute picker (section 2.5).
 */
class SalesRepository
    @Inject
    constructor(
        private val salesApi: SalesApi,
        private val dispatchers: DispatcherProvider,
    ) {
        suspend fun checkout(
            placeId: String,
            lines: List<CartLine>,
            paymentMethod: PaymentMethod,
        ): ApiResult<String> =
            withContext(dispatchers.io) {
                val order =
                    when (val created = apiCall { salesApi.createOrder(CreateOrderRequestDto(placeId)) }) {
                        is ApiResult.Success -> created.value
                        is ApiResult.HttpError -> return@withContext created
                        is ApiResult.NetworkError -> return@withContext created
                    }

                lines.forEach { line ->
                    val added =
                        apiCall {
                            salesApi.addLine(
                                orderId = order.id,
                                body =
                                    OrderLineRequestDto(
                                        productId = line.productId,
                                        productName = line.productName,
                                        quantity = line.quantity,
                                        unitPriceAmount = line.unitPriceGross.minorUnits,
                                    ),
                            )
                        }
                    if (added is ApiResult.HttpError) return@withContext added
                    if (added is ApiResult.NetworkError) return@withContext added
                }

                val finalized =
                    apiCall { salesApi.finalize(order.id, FinalizeOrderRequestDto(paymentMethod.apiValue)) }
                if (finalized is ApiResult.HttpError) return@withContext finalized
                if (finalized is ApiResult.NetworkError) return@withContext finalized

                val receipt =
                    apiCall {
                        salesApi.issueReceipt(
                            IssueReceiptRequestDto(
                                orderId = order.id,
                                placeId = placeId,
                                lines =
                                    lines.map {
                                        DocumentLineDto(
                                            lineId = it.productId,
                                            productId = it.productId,
                                            productName = it.productName,
                                            quantity = it.quantity,
                                            unitPriceAmount = it.unitPriceGross.minorUnits,
                                        )
                                    },
                                totalGrossAmount = lines.total().minorUnits,
                                paymentMethod = paymentMethod.apiValue,
                            ),
                        )
                    }

                when (receipt) {
                    is ApiResult.Success -> ApiResult.Success(receipt.value.id)
                    is ApiResult.HttpError -> receipt
                    is ApiResult.NetworkError -> receipt
                }
            }
    }
