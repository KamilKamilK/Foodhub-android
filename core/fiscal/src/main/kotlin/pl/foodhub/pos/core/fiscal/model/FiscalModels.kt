package pl.foodhub.pos.core.fiscal.model

import java.math.BigDecimal

/**
 * One cart line as needed to fiscalize a sale -- [vatRateIndex] is the fiscal
 * device's own VAT-rate slot (0-6, letters A-G), not a percentage, since both
 * Novitus and Posnet address VAT rates by slot index in their protocols.
 */
data class FiscalLine(
    val name: String,
    val quantity: Int,
    val vatRateIndex: Int,
    val unitPriceGrosz: Long,
    val totalGrosz: Long,
    val discountGrosz: Long? = null,
)

data class FiscalPayment(
    val methodCode: String,
    val amountGrosz: Long,
)

data class FiscalReceiptRequest(
    val lines: List<FiscalLine>,
    val payments: List<FiscalPayment>,
    val buyerNip: String? = null,
)

data class FiscalInvoiceRequest(
    val receipt: FiscalReceiptRequest,
    val buyerName: String,
    val buyerNip: String?,
    val buyerAddress: String?,
)

data class VatRate(
    val index: Int,
    val rate: BigDecimal,
)
