package pl.foodhub.pos.core.printing

import org.junit.Assert.assertTrue
import org.junit.Test

class TicketFormatterTest {
    private val lines =
        listOf(
            PrintableLine(
                productName = "Pizza Margherita",
                quantity = 2,
                orderDirectionId = 1L,
                unitPriceAmount = 2500,
            ),
            PrintableLine(productName = "Cola", quantity = 1, orderDirectionId = 2L, unitPriceAmount = 500),
        )

    @Test
    fun `kitchen ticket has the station name, order reference and quantities but no prices`() {
        val ticket = TicketFormatter.kitchenTicket("Kuchnia", "order-12345678-abcd", lines)

        assertTrue(ticket.any { it.contains("KUCHNIA") })
        assertTrue(ticket.any { it.contains("order-1") })
        assertTrue(ticket.any { it == "2x Pizza Margherita" })
        assertTrue(ticket.any { it == "1x Cola" })
        assertTrue(ticket.none { it.contains("zł") })
    }

    @Test
    fun `receipt has itemized prices, a total and a human payment method label`() {
        val ticket =
            TicketFormatter.receipt(
                "order-12345678-abcd",
                lines,
                totalGrossAmount = 5500,
                paymentMethod = "cash",
            )

        assertTrue(ticket.any { it.contains("order-1") })
        assertTrue(ticket.any { it.contains("2x Pizza Margherita") && it.contains("zł") })
        assertTrue(ticket.any { it.contains("SUMA") && it.contains("55,00") })
        assertTrue(ticket.any { it.contains("Gotowka") })
    }

    @Test
    fun `receipt falls back to the raw payment method value when it is not a known label`() {
        val ticket = TicketFormatter.receipt("order-1", lines, totalGrossAmount = 5500, paymentMethod = "voucher")

        assertTrue(ticket.any { it.contains("voucher") })
    }
}
