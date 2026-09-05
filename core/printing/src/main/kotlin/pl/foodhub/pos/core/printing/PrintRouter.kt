package pl.foodhub.pos.core.printing

import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.network.api.PrintersApi
import pl.foodhub.pos.core.network.apiCall
import pl.foodhub.pos.core.network.model.PrinterDto
import javax.inject.Inject

private const val KITCHEN_ROLE = "KITCHEN"
private const val RECEIPT_ROLE = "RECEIPT"

/** Synthesized status for a printing failure -- deliberately not in the 5xx range (see [PrintRouter]). */
private const val PRINT_FAILURE_STATUS = 400

/**
 * Routes a finalized sale's lines to the place's configured printers over LAN ESC/POS:
 * KITCHEN-role printers each get a station ticket filtered to their assigned order
 * directions (name+qty, no prices); RECEIPT-role printers get the full itemized
 * customer copy (ANDROID_POS_ARCHITECTURE.md Faza 3). The printer list is fetched
 * fresh on every call rather than cached -- a handful of rows per place, cheap, and
 * self-heals after an admin reconfigures a printer's ip/port/role/directions.
 *
 * Any failure (printer list unreachable, or a printer's socket unreachable) is
 * reported as [ApiResult.HttpError] with a non-5xx status rather than
 * [ApiResult.NetworkError]: core:sync's queue processor treats a 5xx/network error as
 * transient and retries the *whole* queue before anything after it runs, which would
 * let one broken printer block every later, unrelated sale from syncing. A lost print
 * is recoverable by staff noticing missing food/paper; a stuck queue is not.
 */
class PrintRouter
    @Inject
    constructor(
        private val printersApi: PrintersApi,
        private val printerClient: LanPrinterClient,
    ) {
        suspend fun printKitchenTickets(
            placeId: String,
            orderId: String,
            lines: List<PrintableLine>,
        ): ApiResult<Unit> =
            printByRole(placeId, KITCHEN_ROLE) { printer ->
                val matchingLines =
                    lines.filter { it.orderDirectionId != null && it.orderDirectionId in printer.orderDirectionIds }
                if (matchingLines.isEmpty()) {
                    true
                } else {
                    sendTicket(printer, TicketFormatter.kitchenTicket(printer.name, orderId, matchingLines))
                }
            }

        suspend fun printReceipt(
            placeId: String,
            orderId: String,
            lines: List<PrintableLine>,
            totalGrossAmount: Long,
            paymentMethod: String,
        ): ApiResult<Unit> =
            printByRole(placeId, RECEIPT_ROLE) { printer ->
                sendTicket(printer, TicketFormatter.receipt(orderId, lines, totalGrossAmount, paymentMethod))
            }

        private suspend fun printByRole(
            placeId: String,
            role: String,
            action: suspend (PrinterDto) -> Boolean,
        ): ApiResult<Unit> {
            val printersResult = apiCall { printersApi.printers(placeId) }
            val printers =
                (printersResult as? ApiResult.Success)?.value
                    ?: return ApiResult.HttpError(PRINT_FAILURE_STATUS, "PRINTER_LIST_UNAVAILABLE", null)

            // map-then-all (not a single .all { action(it) }) so a failure on one printer
            // doesn't short-circuit and skip sending to the rest.
            val allSucceeded = printers.filter { it.role == role }.map { action(it) }.all { it }
            return if (allSucceeded) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.HttpError(PRINT_FAILURE_STATUS, "PRINT_FAILED", null)
            }
        }

        private suspend fun sendTicket(
            printer: PrinterDto,
            ticketLines: List<String>,
        ): Boolean {
            val port = printer.port.toIntOrNull() ?: return false
            return printerClient.send(printer.ip, port, EscPosEncoder.encode(ticketLines)).isSuccess
        }
    }
