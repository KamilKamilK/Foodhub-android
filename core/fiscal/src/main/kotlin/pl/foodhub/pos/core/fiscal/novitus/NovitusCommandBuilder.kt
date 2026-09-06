package pl.foodhub.pos.core.fiscal.novitus

import pl.foodhub.pos.core.fiscal.model.FiscalLine
import pl.foodhub.pos.core.fiscal.model.FiscalPayment
import java.time.LocalDate

private const val FIELD_SEP = ";"
private const val VALUE_SEP = "/"
private const val CR = "\r"
private const val GROSZ_PER_UNIT = 100
private const val VAT_LETTER_OFFSET = 'A'.code

/**
 * Builds the field content for each Novitus command this driver actually uses --
 * mnemonic placement and field order follow the manufacturer's protocol
 * documentation and should be confirmed against it before running against real
 * hardware; no command path is built here that nothing in [pl.foodhub.pos.core.fiscal
 * .novitus.NovitusFiscalDriver] calls.
 */
object NovitusCommandBuilder {
    /** `$e` -- cancels a dangling transaction, issued defensively before every fiscalization. */
    fun cancelTransaction(): ByteArray = "0\$e".toByteArray(Charsets.US_ASCII)

    /** `$h` -- begins a fiscal transaction, optionally carrying the buyer's NIP for an imienny receipt. */
    fun beginTransaction(buyerNip: String?): ByteArray {
        val nipFlag = if (buyerNip != null) "1" else "0"
        val header = "0$FIELD_SEP" + "0$FIELD_SEP" + "0$FIELD_SEP$nipFlag\$h"
        return (if (buyerNip != null) "$header$buyerNip$CR" else header).toByteArray(Charsets.US_ASCII)
    }

    /** `$l` -- prints one fiscal line item. */
    fun printLine(line: FiscalLine): ByteArray {
        val vatChar = (VAT_LETTER_OFFSET + line.vatRateIndex).toChar()
        val discount = line.discountGrosz?.let { "$VALUE_SEP${it.toPriceString()}" }.orEmpty()
        val content =
            "${line.name}$CR${line.quantity}$CR$vatChar$VALUE_SEP" +
                "${line.unitPriceGrosz.toPriceString()}$VALUE_SEP${line.totalGrosz.toPriceString()}$discount\$l"
        return content.toByteArray(Charsets.US_ASCII)
    }

    /** `$b` -- registers one payment form against the open transaction. */
    fun addPayment(payment: FiscalPayment): ByteArray =
        "1$FIELD_SEP${payment.methodCode}\$b${payment.amountGrosz.toPriceString()}$VALUE_SEP$CR".toByteArray(
            Charsets.US_ASCII,
        )

    /** `$y` -- commits the transaction to fiscal memory. */
    fun confirmPayment(
        cashierName: String,
        totalGrosz: Long,
        paidGrosz: Long,
    ): ByteArray =
        "$cashierName$CR${totalGrosz.toPriceString()}$VALUE_SEP${paidGrosz.toPriceString()}\$y".toByteArray(
            Charsets.US_ASCII,
        )

    /** `$d` -- opens the cash drawer. */
    fun openDrawer(): ByteArray = "1\$d".toByteArray(Charsets.US_ASCII)

    /** `#n` -- fetches the last error's detail text. */
    fun lastError(): ByteArray = "#n".toByteArray(Charsets.US_ASCII)

    /** `#s` -- fetches fiscal info, including the 7-slot (A-G) VAT-rate table. */
    fun fiscalInfo(): ByteArray = "#s".toByteArray(Charsets.US_ASCII)

    /** `#r` -- daily fiscal report. */
    fun dailyReport(
        date: LocalDate,
        cashierName: String,
    ): ByteArray {
        val ymd = "${date.year % GROSZ_PER_UNIT}$FIELD_SEP${date.monthValue}$FIELD_SEP${date.dayOfMonth}"
        return "1$FIELD_SEP$ymd#r$cashierName$CR".toByteArray(Charsets.US_ASCII)
    }

    /** `#o` -- monthly fiscal report over a date range. */
    fun monthlyReport(
        from: LocalDate,
        to: LocalDate,
        cashierName: String,
    ): ByteArray {
        val fromYmd = "${from.year % GROSZ_PER_UNIT}$FIELD_SEP${from.monthValue}$FIELD_SEP${from.dayOfMonth}"
        val toYmd = "${to.year % GROSZ_PER_UNIT}$FIELD_SEP${to.monthValue}$FIELD_SEP${to.dayOfMonth}"
        return "$fromYmd$FIELD_SEP${toYmd}${FIELD_SEP}6#o$cashierName$CR".toByteArray(Charsets.US_ASCII)
    }

    private fun Long.toPriceString(): String {
        val whole = this / GROSZ_PER_UNIT
        val fraction = (kotlin.math.abs(this) % GROSZ_PER_UNIT).toString().padStart(2, '0')
        return "$whole.$fraction"
    }
}
