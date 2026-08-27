package pl.foodhub.pos.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethodDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("active") val active: Boolean = true,
)

@Serializable
data class CreateOrderRequestDto(
    @SerialName("placeId") val placeId: String,
)

@Serializable
data class OrderLineRequestDto(
    @SerialName("productId") val productId: String,
    @SerialName("productName") val productName: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("unitPriceAmount") val unitPriceAmount: Long,
    @SerialName("unitPriceCurrency") val unitPriceCurrency: String = "PLN",
    @SerialName("discountId") val discountId: String? = null,
    @SerialName("discountAmount") val discountAmount: Long = 0,
)

@Serializable
data class FinalizeOrderRequestDto(
    @SerialName("paymentMethod") val paymentMethod: String,
)

@Serializable
data class OrderDto(
    @SerialName("id") val id: String,
    @SerialName("placeId") val placeId: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("totalGross") val totalGross: Long = 0,
)

@Serializable
data class DocumentLineDto(
    @SerialName("lineId") val lineId: String,
    @SerialName("productId") val productId: String,
    @SerialName("productName") val productName: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("unitPriceAmount") val unitPriceAmount: Long,
    @SerialName("unitPriceCurrency") val unitPriceCurrency: String = "PLN",
    @SerialName("discountId") val discountId: String? = null,
    @SerialName("discountAmount") val discountAmount: Long = 0,
)

@Serializable
data class IssueReceiptRequestDto(
    @SerialName("orderId") val orderId: String,
    @SerialName("placeId") val placeId: String,
    @SerialName("lines") val lines: List<DocumentLineDto>,
    @SerialName("totalGrossAmount") val totalGrossAmount: Long,
    @SerialName("currency") val currency: String = "PLN",
    @SerialName("paymentMethod") val paymentMethod: String,
    // Attribute values captured at issue time (occurrence = sales_documents),
    // fed to the "sales by attributes" report -- ANDROID_POS_ARCHITECTURE.md 2.5.
    @SerialName("attributeValueIds") val attributeValueIds: List<Int> = emptyList(),
)

@Serializable
data class IssueInvoiceRequestDto(
    @SerialName("orderId") val orderId: String,
    @SerialName("placeId") val placeId: String,
    @SerialName("buyerName") val buyerName: String,
    @SerialName("buyerNip") val buyerNip: String,
    @SerialName("lines") val lines: List<DocumentLineDto>,
    @SerialName("totalGrossAmount") val totalGrossAmount: Long,
    @SerialName("currency") val currency: String = "PLN",
    @SerialName("paymentMethod") val paymentMethod: String,
    @SerialName("dueDate") val dueDate: String,
    @SerialName("attributeValueIds") val attributeValueIds: List<Int> = emptyList(),
)

@Serializable
data class SalesDocumentDto(
    @SerialName("id") val id: String,
    @SerialName("orderId") val orderId: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("totalGrossAmount") val totalGrossAmount: Long = 0,
    @SerialName("paymentMethod") val paymentMethod: String = "",
)

@Serializable
data class SalesAttributeDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String = "",
    @SerialName("values") val values: List<SalesAttributeValueDto> = emptyList(),
)

@Serializable
data class SalesAttributeValueDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String = "",
)
