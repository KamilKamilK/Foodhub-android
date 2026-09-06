package pl.foodhub.pos.core.fiscal.posnet

import pl.foodhub.pos.core.fiscal.model.FiscalLine
import pl.foodhub.pos.core.fiscal.model.FiscalPayment
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd")

/**
 * Builds the tab-joined token list for each Posnet online command this driver
 * actually uses -- field names/order follow the manufacturer's protocol
 * documentation and should be confirmed against it before running against real
 * hardware; no command path is built here that nothing in [pl.foodhub.pos.core.fiscal
 * .posnet.PosnetFiscalDriver] calls.
 */
object PosnetCommandBuilder {
    fun initTransaction(): List<String> = listOf("trinit", "bm=1")

    fun line(line: FiscalLine): List<String> {
        val tokens =
            mutableListOf(
                "trline",
                "na=${line.name}",
                "vt=${line.vatRateIndex}",
                "pr=${line.unitPriceGrosz}",
                "il=${line.quantity}",
                "st=n",
            )
        line.discountGrosz?.let { tokens += "rd=$it" }
        return tokens
    }

    fun payment(payment: FiscalPayment): List<String> =
        listOf(
            "trpayment",
            "ty=${payment.methodCode}",
            "wa=${payment.amountGrosz}",
        )

    fun setNip(nip: String): List<String> = listOf("trnipset", "ni=$nip")

    fun setFooter(cashierName: String): List<String> = listOf("ftrcfg", "cc=$cashierName")

    fun endTransaction(
        totalGrosz: Long,
        paymentGrosz: Long?,
        isInvoice: Boolean,
    ): List<String> {
        val tokens = mutableListOf("trend", "to=$totalGrosz")
        if (!isInvoice && paymentGrosz != null) tokens += "fp=$paymentGrosz"
        return tokens
    }

    fun feed(lines: Int): List<String> = listOf("papfeed", "ln=$lines")

    fun openDrawer(): List<String> = listOf("opendrwr")

    fun cancelTransaction(): List<String> = listOf("prncancel")

    fun vatRates(): List<String> = listOf("vatget")

    fun documentCounters(): List<String> = listOf("scnt")

    fun dailyReport(date: LocalDate): List<String> = listOf("dailyrep", "da=${date.format(DATE_FORMAT)}")

    fun monthlyReport(date: LocalDate): List<String> = listOf("monthlyrep", "da=${date.format(DATE_FORMAT)}")

    fun mechanismStatus(): List<String> = listOf("!sprn")

    fun deviceStatus(): List<String> = listOf("!sdev")

    fun initInvoice(): List<String> = listOf("trfvinit")

    fun invoiceBuyer(
        buyerName: String,
        buyerNip: String?,
        buyerAddress: String?,
    ): List<String> {
        val tokens = mutableListOf("trfvbuyer", "na=$buyerName")
        buyerNip?.let { tokens += "ni=$it" }
        buyerAddress?.let { tokens += "ad=$it" }
        return tokens
    }
}
