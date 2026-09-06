package pl.foodhub.pos.core.fiscal.novitus

import pl.foodhub.pos.core.fiscal.model.VatRate
import java.math.BigDecimal

private const val MAX_VAT_SLOTS = 7

/**
 * Parses the `#s` fiscal-info response's `;`-delimited numeric fields into the
 * device's 7 VAT-rate slots (A-G) -- exact field positions to confirm against the
 * manufacturer's protocol documentation; fields that aren't valid decimals are
 * skipped rather than failing the whole parse.
 */
object NovitusVatRateParser {
    fun parse(response: ByteArray): List<VatRate> =
        response
            .toString(Charsets.US_ASCII)
            .split(";")
            .mapNotNull { it.trim().toBigDecimalOrNull() }
            .take(MAX_VAT_SLOTS)
            .mapIndexed { index, rate -> VatRate(index, rate) }

    private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()
}
