package pl.foodhub.pos.core.printing

import pl.foodhub.pos.core.common.Money

private const val SEPARATOR = "--------------------------------"
private const val ORDER_REF_LENGTH = 8

private val PAYMENT_METHOD_LABELS =
    mapOf(
        "cash" to "Gotowka",
        "card" to "Karta",
        "bank_transfer" to "Przelew",
    )

/**
 * Builds the plain-text line lists [EscPosEncoder] turns into bytes. Two distinct
 * documents, per ANDROID_POS_ARCHITECTURE.md section 10: a kitchen/bar ticket (names
 * and quantities only -- staff preparing food doesn't need prices) and the customer
 * receipt copy (full itemized lines, total, payment method).
 */
object TicketFormatter {
    fun kitchenTicket(
        stationName: String,
        orderId: String,
        lines: List<PrintableLine>,
    ): List<String> =
        buildList {
            add(stationName.uppercase())
            add("Zamowienie: ${orderId.take(ORDER_REF_LENGTH)}")
            add(SEPARATOR)
            lines.forEach { line -> add("${line.quantity}x ${line.productName}") }
        }

    fun receipt(
        orderId: String,
        lines: List<PrintableLine>,
        totalGrossAmount: Long,
        paymentMethod: String,
    ): List<String> =
        buildList {
            add("PARAGON")
            add("Nr: ${orderId.take(ORDER_REF_LENGTH)}")
            add(SEPARATOR)
            lines.forEach { line ->
                val lineTotal = Money(line.unitPriceAmount * line.quantity).formatPln()
                add("${line.quantity}x ${line.productName} - $lineTotal")
            }
            add(SEPARATOR)
            add("SUMA: ${Money(totalGrossAmount).formatPln()}")
            add("Platnosc: ${PAYMENT_METHOD_LABELS[paymentMethod] ?: paymentMethod}")
        }
}
