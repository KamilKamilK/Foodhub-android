package pl.foodhub.pos.core.network.api

import pl.foodhub.pos.core.network.model.CreateOrderRequestDto
import pl.foodhub.pos.core.network.model.FinalizeOrderRequestDto
import pl.foodhub.pos.core.network.model.IssueInvoiceRequestDto
import pl.foodhub.pos.core.network.model.IssueReceiptRequestDto
import pl.foodhub.pos.core.network.model.OrderDto
import pl.foodhub.pos.core.network.model.OrderLineRequestDto
import pl.foodhub.pos.core.network.model.PaymentMethodDto
import pl.foodhub.pos.core.network.model.SalesAttributeDto
import pl.foodhub.pos.core.network.model.SalesDocumentDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Checkout is a sequence of calls against the DDD order/document endpoints
 * (ANDROID_POS_ARCHITECTURE.md "Kontrakt REST"):
 * orders -> lines -> confirm|finalize -> receipts|invoices. Faza 1 is online-only;
 * the offline write-ahead queue lands in Faza 2 (core:sync).
 */
interface SalesApi {
    @GET("v1/payment-methods")
    suspend fun paymentMethods(): List<PaymentMethodDto>

    @GET("v1/attributes")
    suspend fun salesAttributes(
        @Query("occurrence") occurrence: String = "sales_documents",
    ): List<SalesAttributeDto>

    @POST("v1/order/orders")
    suspend fun createOrder(
        @Body body: CreateOrderRequestDto,
    ): OrderDto

    @POST("v1/order/orders/{orderId}/lines")
    suspend fun addLine(
        @Path("orderId") orderId: String,
        @Body body: OrderLineRequestDto,
    ): OrderDto

    @PUT("v1/order/orders/{orderId}/confirm")
    suspend fun confirm(
        @Path("orderId") orderId: String,
    ): OrderDto

    @PUT("v1/order/orders/{orderId}/finalize")
    suspend fun finalize(
        @Path("orderId") orderId: String,
        @Body body: FinalizeOrderRequestDto,
    ): OrderDto

    @POST("v1/order/receipts")
    suspend fun issueReceipt(
        @Body body: IssueReceiptRequestDto,
    ): SalesDocumentDto

    @POST("v1/order/invoices")
    suspend fun issueInvoice(
        @Body body: IssueInvoiceRequestDto,
    ): SalesDocumentDto
}
