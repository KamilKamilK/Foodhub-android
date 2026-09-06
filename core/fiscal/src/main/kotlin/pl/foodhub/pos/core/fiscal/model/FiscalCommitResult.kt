package pl.foodhub.pos.core.fiscal.model

/**
 * Outcome of a fiscal-memory commit attempt. [Ambiguous] exists so a driver never has
 * to guess when a device's response doesn't parse cleanly -- silently treating an
 * ambiguous response as success (or as a specific failure) would risk either a lost
 * legal document or a false alarm; the caller decides how to handle "unclear" instead.
 */
sealed interface FiscalCommitResult {
    data class Success(val fiscalDocumentNumber: String, val dailyReportNumber: String?) : FiscalCommitResult

    data class DeviceError(val code: String?, val message: String) : FiscalCommitResult

    data object Unreachable : FiscalCommitResult

    data object Ambiguous : FiscalCommitResult
}
