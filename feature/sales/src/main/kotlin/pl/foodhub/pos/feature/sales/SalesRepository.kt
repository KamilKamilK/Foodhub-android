package pl.foodhub.pos.feature.sales

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pl.foodhub.pos.core.auth.AuthRepository
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.common.map
import pl.foodhub.pos.core.fiscal.FiscalCoordinator
import pl.foodhub.pos.core.fiscal.FiscalOutcome
import pl.foodhub.pos.core.fiscal.VatRateSlotMapper
import pl.foodhub.pos.core.fiscal.model.FiscalInvoiceRequest
import pl.foodhub.pos.core.fiscal.model.FiscalLine
import pl.foodhub.pos.core.fiscal.model.FiscalPayment
import pl.foodhub.pos.core.fiscal.model.FiscalReceiptRequest
import pl.foodhub.pos.core.network.api.SalesApi
import pl.foodhub.pos.core.network.apiCall
import pl.foodhub.pos.core.network.model.DocumentLineDto
import pl.foodhub.pos.core.network.model.FinalizeOrderRequestDto
import pl.foodhub.pos.core.network.model.FiscalizeInvoiceRequestDto
import pl.foodhub.pos.core.network.model.FiscalizeReceiptRequestDto
import pl.foodhub.pos.core.network.model.IssueInvoiceRequestDto
import pl.foodhub.pos.core.network.model.IssueReceiptRequestDto
import pl.foodhub.pos.core.network.model.OrderLineRequestDto
import pl.foodhub.pos.core.printing.PrintableLine
import pl.foodhub.pos.core.sync.SyncQueue
import java.time.Instant
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

/** The identifiers a single checkout call carries end-to-end, bundled to keep call sites short. */
private data class CheckoutIdentifiers(
    val documentId: String,
    val orderId: String,
    val placeId: String,
    val posId: String?,
)

/** How to finalize a sale: payment, receipt vs. invoice, and the section 2.5 attribute picker. */
data class CheckoutOptions(
    val paymentMethod: PaymentMethod,
    val invoiceDetails: InvoiceDetails?,
    val attributeValueIds: List<Int>,
)

/**
 * Outcome of [SalesRepository.checkout]. Unlike every other step (queued,
 * fire-and-forget), a terminal with a configured fiscal device blocks on the
 * hardware commit -- see [FiscalCoordinator] -- so checkout can, for the first
 * time, genuinely fail: [FiscalDeviceFailure] means nothing after the fiscal step
 * was queued (confirm/finalize already were, safely, and retrying `checkout()` for
 * the same `orderId` after fixing the printer is safe).
 */
sealed interface CheckoutResult {
    data object Success : CheckoutResult

    data class FiscalDeviceFailure(val reason: String) : CheckoutResult
}

/**
 * Queues the checkout sequence -- add lines, confirm, finalize, issue a receipt or an
 * invoice when the buyer supplied a NIP, then print the kitchen/bar tickets and the
 * customer receipt copy -- through [SyncQueue] instead of calling the network
 * directly, so a connectivity drop mid-checkout never loses the sale
 * (ANDROID_POS_ARCHITECTURE.md section 9 point 2, closing this class's former Faza 2
 * TODO). Every step's id (line/receipt/invoice) is generated here so a queued retry
 * after a dropped response is a backend no-op rather than a duplicate (section 9
 * point 4).
 *
 * When the terminal's Pos has a configured `FiscalDevice` (Faza 5), the hardware
 * fiscal-memory commit runs synchronously here, before the document is issued or
 * anything is queued for printing -- see [FiscalCoordinator] for why this one step
 * cannot go through the offline queue like everything else. On success, the fiscal
 * device's own printout is the legal receipt, so the plain ESC/POS receipt copy is
 * skipped (kitchen tickets are unaffected either way). A terminal with no
 * `FiscalDevice` configured yet behaves exactly as before Faza 5.
 */
class SalesRepository
    @Inject
    constructor(
        private val salesApi: SalesApi,
        private val syncQueue: SyncQueue,
        private val authRepository: AuthRepository,
        private val fiscalCoordinator: FiscalCoordinator,
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
        ): CheckoutResult =
            withContext(dispatchers.io) {
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

                val posId = authRepository.posSession.first()?.posId
                val ids =
                    CheckoutIdentifiers(
                        documentId = UUID.randomUUID().toString(),
                        orderId = orderId,
                        placeId = placeId,
                        posId = posId,
                    )

                val fiscalOutcome =
                    if (posId != null) {
                        fiscalize(placeId, posId, ids.documentId, lines, options)
                    } else {
                        FiscalOutcome.NotConfigured
                    }

                if (fiscalOutcome is FiscalOutcome.Failed) {
                    return@withContext CheckoutResult.FiscalDeviceFailure(fiscalOutcome.reason)
                }

                issueDocument(ids, lines, options)

                val printableLines = lines.toPrintableLines()
                syncQueue.printKitchenTickets(orderId, placeId, printableLines)

                when (fiscalOutcome) {
                    is FiscalOutcome.Fiscalized ->
                        recordFiscalization(
                            ids.documentId,
                            options.invoiceDetails != null,
                            fiscalOutcome,
                        )
                    FiscalOutcome.NotConfigured ->
                        syncQueue.printReceipt(
                            orderId,
                            placeId,
                            printableLines,
                            lines.total().minorUnits,
                            options.paymentMethod.apiValue,
                        )
                    is FiscalOutcome.Failed -> Unit // handled above; unreachable here
                }

                CheckoutResult.Success
            }

        private suspend fun fiscalize(
            placeId: String,
            posId: String,
            documentId: String,
            lines: List<CartLine>,
            options: CheckoutOptions,
        ): FiscalOutcome {
            val receiptRequest = lines.toFiscalReceiptRequest(options)
            val invoiceDetails = options.invoiceDetails
            return if (invoiceDetails != null) {
                fiscalCoordinator.fiscalizeInvoice(
                    placeId,
                    posId,
                    documentId,
                    FiscalInvoiceRequest(
                        receiptRequest,
                        invoiceDetails.buyerName,
                        invoiceDetails.buyerNip,
                        buyerAddress = null,
                    ),
                )
            } else {
                fiscalCoordinator.fiscalizeReceipt(placeId, posId, documentId, receiptRequest)
            }
        }

        private suspend fun recordFiscalization(
            documentId: String,
            isInvoice: Boolean,
            outcome: FiscalOutcome.Fiscalized,
        ) {
            val fiscalizedAt = Instant.now().toString()
            if (isInvoice) {
                syncQueue.recordInvoiceFiscalization(
                    documentId,
                    FiscalizeInvoiceRequestDto(
                        outcome.fiscalDeviceId,
                        outcome.fiscalDocumentNumber,
                        outcome.dailyReportNumber,
                        fiscalizedAt,
                    ),
                )
            } else {
                syncQueue.recordReceiptFiscalization(
                    documentId,
                    FiscalizeReceiptRequestDto(
                        outcome.fiscalDeviceId,
                        outcome.fiscalDocumentNumber,
                        outcome.dailyReportNumber,
                        fiscalizedAt,
                    ),
                )
            }
        }

        private suspend fun issueDocument(
            ids: CheckoutIdentifiers,
            lines: List<CartLine>,
            options: CheckoutOptions,
        ) {
            val (paymentMethod, invoiceDetails, attributeValueIds) = options
            val documentLines = lines.toDocumentLines()
            val totalGrossAmount = lines.total().minorUnits

            if (invoiceDetails != null) {
                syncQueue.issueInvoice(
                    IssueInvoiceRequestDto(
                        orderId = ids.orderId,
                        placeId = ids.placeId,
                        buyerName = invoiceDetails.buyerName,
                        buyerNip = invoiceDetails.buyerNip,
                        lines = documentLines,
                        totalGrossAmount = totalGrossAmount,
                        paymentMethod = paymentMethod.apiValue,
                        dueDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        attributeValueIds = attributeValueIds,
                        invoiceId = ids.documentId,
                        posId = ids.posId,
                    ),
                )
            } else {
                syncQueue.issueReceipt(
                    IssueReceiptRequestDto(
                        orderId = ids.orderId,
                        placeId = ids.placeId,
                        lines = documentLines,
                        totalGrossAmount = totalGrossAmount,
                        paymentMethod = paymentMethod.apiValue,
                        attributeValueIds = attributeValueIds,
                        receiptId = ids.documentId,
                        posId = ids.posId,
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

        private fun List<CartLine>.toPrintableLines(): List<PrintableLine> =
            map {
                PrintableLine(
                    productName = it.productName,
                    quantity = it.quantity,
                    orderDirectionId = it.orderDirectionId,
                    unitPriceAmount = it.unitPriceGross.minorUnits,
                )
            }

        private fun List<CartLine>.toFiscalReceiptRequest(options: CheckoutOptions): FiscalReceiptRequest {
            val fiscalLines =
                map {
                    FiscalLine(
                        name = it.productName,
                        quantity = it.quantity,
                        vatRateIndex = VatRateSlotMapper.slotFor(it.taxRateValue),
                        unitPriceGrosz = it.unitPriceGross.minorUnits,
                        totalGrosz = it.lineGross.minorUnits,
                    )
                }
            val payment = FiscalPayment(options.paymentMethod.apiValue, total().minorUnits)
            return FiscalReceiptRequest(fiscalLines, listOf(payment), options.invoiceDetails?.buyerNip)
        }
    }
