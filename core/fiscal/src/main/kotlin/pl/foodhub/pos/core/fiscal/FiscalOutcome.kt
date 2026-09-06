package pl.foodhub.pos.core.fiscal

sealed interface FiscalOutcome {
    /** No `FiscalDevice` configured for this Pos yet -- checkout proceeds exactly as before Faza 5. */
    data object NotConfigured : FiscalOutcome

    data class Fiscalized(
        val fiscalDeviceId: String,
        val fiscalDocumentNumber: String,
        val dailyReportNumber: String?,
    ) : FiscalOutcome

    data class Failed(val reason: String) : FiscalOutcome
}
