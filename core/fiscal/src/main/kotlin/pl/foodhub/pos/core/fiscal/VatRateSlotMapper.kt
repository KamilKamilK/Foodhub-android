package pl.foodhub.pos.core.fiscal

/**
 * Maps a VAT percentage (as already resolved server-side and cached on the menu
 * item, see `foodhub-api`'s `TaxRateReadModelRepositoryInterface`) to the fiscal
 * device's VAT-rate slot (0-6, letters A-G). Polish fiscal devices follow a
 * standard, tax-law-driven slot convention for retail sales -- A=23%, B=8%, C=5%,
 * D=0%, E=exempt -- rather than an arbitrary per-device mapping, so this is safe to
 * hardcode; it is not something the terminal can otherwise discover on its own (a
 * device's `vatget`/`#s` response gives the *rates currently in each slot*, not a
 * documented "which slot is which rate" contract to probe against per sale).
 */
object VatRateSlotMapper {
    private const val SLOT_A_STANDARD = 23.0
    private const val SLOT_B_REDUCED = 8.0
    private const val SLOT_C_REDUCED = 5.0
    private const val SLOT_D_ZERO = 0.0

    private const val SLOT_INDEX_A = 0
    private const val SLOT_INDEX_B = 1
    private const val SLOT_INDEX_C = 2
    private const val SLOT_INDEX_D = 3

    /** E -- exempt/other, the conventional catch-all slot. */
    private const val SLOT_INDEX_E = 4

    fun slotFor(vatRatePercent: Double): Int =
        when (vatRatePercent) {
            SLOT_A_STANDARD -> SLOT_INDEX_A
            SLOT_B_REDUCED -> SLOT_INDEX_B
            SLOT_C_REDUCED -> SLOT_INDEX_C
            SLOT_D_ZERO -> SLOT_INDEX_D
            else -> SLOT_INDEX_E
        }
}
