package pl.foodhub.pos.core.sync

import kotlinx.serialization.Serializable
import pl.foodhub.pos.core.network.model.FinalizeOrderRequestDto
import pl.foodhub.pos.core.network.model.OrderLineRequestDto

/**
 * Small wrapper payloads for the operations whose path params ([OCCUPY_TABLE],
 * [RELEASE_TABLE], [ADD_ORDER_LINE], [CONFIRM_ORDER], [FINALIZE_ORDER]) aren't already
 * part of an existing `core:network` request body. `CREATE_ORDER`/`ISSUE_RECEIPT`/
 * `ISSUE_INVOICE` reuse `CreateOrderRequestDto`/`IssueReceiptRequestDto`/
 * `IssueInvoiceRequestDto` directly as their payload -- no wrapper needed.
 */
@Serializable
data class OccupyTablePayload(val tableId: String, val orderId: String)

@Serializable
data class ReleaseTablePayload(val tableId: String, val orderId: String)

@Serializable
data class AddOrderLinePayload(val orderId: String, val request: OrderLineRequestDto)

@Serializable
data class ConfirmOrderPayload(val orderId: String)

@Serializable
data class FinalizeOrderPayload(val orderId: String, val request: FinalizeOrderRequestDto)
