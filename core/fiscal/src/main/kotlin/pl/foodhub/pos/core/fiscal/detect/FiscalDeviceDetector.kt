package pl.foodhub.pos.core.fiscal.detect

import pl.foodhub.pos.core.fiscal.FiscalManufacturer
import pl.foodhub.pos.core.fiscal.FiscalPrinterDriver
import pl.foodhub.pos.core.fiscal.model.FiscalProbeResult
import javax.inject.Inject

/**
 * A "test connection" diagnostic for setting up a `FiscalDevice`, not a per-sale
 * dispatch step -- once a device is configured, checkout routes to it deterministically
 * by [pl.foodhub.pos.core.fiscal.FiscalDriverFactory], no guessing per sale. Probes every
 * known driver in turn and never falls back to a specific manufacturer on failure.
 */
class FiscalDeviceDetector
    @Inject
    constructor(private val drivers: @JvmSuppressWildcards Map<FiscalManufacturer, FiscalPrinterDriver>) {
        suspend fun detect(
            ip: String,
            port: Int,
        ): FiscalProbeResult {
            for (driver in drivers.values) {
                val result = driver.probe(ip, port)
                if (result is FiscalProbeResult.Detected) return result
            }
            return FiscalProbeResult.Unreachable
        }
    }
