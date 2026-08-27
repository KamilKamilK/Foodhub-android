package pl.foodhub.pos.core.common

/**
 * A gross amount in minor units (grosze). The `foodhub-api` order/receipt contract
 * works entirely in integer minor units (`unitPriceAmount`, `totalGrossAmount`);
 * keeping the same representation on the terminal avoids rounding drift between the
 * cart total shown to the cashier and the document the backend issues.
 */
@JvmInline
value class Money(val minorUnits: Long) {
    operator fun plus(other: Money) = Money(minorUnits + other.minorUnits)

    operator fun times(quantity: Int) = Money(minorUnits * quantity)

    fun formatPln(): String {
        val sign = if (minorUnits < 0) "-" else ""
        val abs = kotlin.math.abs(minorUnits)
        val fraction = (abs % MINOR_UNITS_PER_UNIT).toString().padStart(FRACTION_DIGITS, '0')
        return "$sign${abs / MINOR_UNITS_PER_UNIT},$fraction zł"
    }

    companion object {
        val ZERO = Money(0)

        private const val MINOR_UNITS_PER_UNIT = 100
        private const val FRACTION_DIGITS = 2
    }
}
