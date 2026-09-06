package pl.foodhub.pos.core.fiscal

import pl.foodhub.pos.core.fiscal.model.FiscalCommitResult
import pl.foodhub.pos.core.fiscal.model.FiscalInvoiceRequest
import pl.foodhub.pos.core.fiscal.model.FiscalProbeResult
import pl.foodhub.pos.core.fiscal.model.FiscalReceiptRequest
import pl.foodhub.pos.core.fiscal.model.VatRate
import java.time.LocalDate

/**
 * One implementation per fiscal-printer manufacturer (Novitus, Posnet online) --
 * selected per [FiscalManufacturer] by [pl.foodhub.pos.core.fiscal.di.FiscalDriverFactory],
 * per the device configured for the terminal's Pos (`GET
 * v1/places/{placeId}/pos/{posId}/fiscal-device`).
 */
interface FiscalPrinterDriver {
    val manufacturer: FiscalManufacturer

    suspend fun probe(
        ip: String,
        port: Int,
    ): FiscalProbeResult

    suspend fun fiscalizeReceipt(
        ip: String,
        port: Int,
        request: FiscalReceiptRequest,
    ): FiscalCommitResult

    suspend fun fiscalizeInvoice(
        ip: String,
        port: Int,
        request: FiscalInvoiceRequest,
    ): FiscalCommitResult

    suspend fun openDrawer(
        ip: String,
        port: Int,
    ): Result<Unit>

    suspend fun dailyReport(
        ip: String,
        port: Int,
        cashierName: String,
    ): Result<Unit>

    suspend fun monthlyReport(
        ip: String,
        port: Int,
        from: LocalDate,
        to: LocalDate,
        cashierName: String,
    ): Result<Unit>

    suspend fun vatRates(
        ip: String,
        port: Int,
    ): Result<List<VatRate>>

    suspend fun receiptCount(
        ip: String,
        port: Int,
    ): Result<Int>
}
