package pl.foodhub.pos.core.fiscal

import javax.inject.Inject

class FiscalDriverFactory
    @Inject
    constructor(private val drivers: @JvmSuppressWildcards Map<FiscalManufacturer, FiscalPrinterDriver>) {
        fun forManufacturer(manufacturer: FiscalManufacturer): FiscalPrinterDriver = drivers.getValue(manufacturer)
    }
