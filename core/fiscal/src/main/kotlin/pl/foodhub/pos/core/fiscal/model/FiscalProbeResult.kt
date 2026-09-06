package pl.foodhub.pos.core.fiscal.model

import pl.foodhub.pos.core.fiscal.FiscalManufacturer

/**
 * Outcome of probing a device to find out which protocol it speaks. Deliberately has
 * no "unknown, assume a default manufacturer" case -- misrouting fiscal commands to
 * the wrong protocol is worse than surfacing a clear "not detected" error to the
 * operator.
 */
sealed interface FiscalProbeResult {
    data class Detected(val manufacturer: FiscalManufacturer) : FiscalProbeResult

    data object Unreachable : FiscalProbeResult

    data object Ambiguous : FiscalProbeResult
}
