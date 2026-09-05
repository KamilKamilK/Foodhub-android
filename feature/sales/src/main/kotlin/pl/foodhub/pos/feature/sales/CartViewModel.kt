package pl.foodhub.pos.feature.sales

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.foodhub.pos.core.auth.AuthRepository
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.common.Money
import pl.foodhub.pos.core.database.MenuCacheDao
import pl.foodhub.pos.core.sync.SyncQueue
import javax.inject.Inject

data class PickerProduct(
    val productId: String,
    val name: String,
    val unitPriceGross: Money,
)

data class CartUiState(
    val lines: List<CartLine> = emptyList(),
    val availableAttributes: List<SalesAttribute> = emptyList(),
    val selectedAttributeValueIds: Set<Int> = emptySet(),
    val invoiceRequested: Boolean = false,
    val buyerName: String = "",
    val buyerNip: String = "",
    val submitting: Boolean = false,
    val queuedForSync: Boolean = false,
    val error: Boolean = false,
) {
    val total: Money get() = lines.total()

    val canCheckout: Boolean
        get() {
            val invoiceReady = !invoiceRequested || (buyerName.isNotBlank() && isValidNip(buyerNip))
            return lines.isNotEmpty() && !submitting && invoiceReady
        }
}

private fun isValidNip(nip: String) = nip.length == NIP_LENGTH && nip.all(Char::isDigit)

private const val NIP_LENGTH = 10

@HiltViewModel
class CartViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val salesRepository: SalesRepository,
        private val authRepository: AuthRepository,
        private val syncQueue: SyncQueue,
        menuCacheDao: MenuCacheDao,
    ) : ViewModel() {
        private val orderId: String = checkNotNull(savedStateHandle["orderId"])
        private val tableId: String = checkNotNull(savedStateHandle["tableId"])

        private val _state = MutableStateFlow(CartUiState())
        val state = _state.asStateFlow()

        val availableProducts =
            menuCacheDao.observeItems()
                .map { items ->
                    items
                        .filter { it.productId.isNotBlank() }
                        .map { PickerProduct(it.productId, it.productName, Money(it.unitPriceGrossMinor)) }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        init {
            viewModelScope.launch {
                val result = salesRepository.attributes()
                if (result is ApiResult.Success) {
                    _state.update { it.copy(availableAttributes = result.value) }
                }
            }
        }

        fun addProduct(product: PickerProduct) {
            _state.update { current ->
                val existing = current.lines.indexOfFirst { it.productId == product.productId }
                val lines =
                    if (existing >= 0) {
                        current.lines.mapIndexed { index, line ->
                            if (index == existing) line.copy(quantity = line.quantity + 1) else line
                        }
                    } else {
                        current.lines +
                            CartLine(product.productId, product.name, product.unitPriceGross, quantity = 1)
                    }
                current.copy(lines = lines, error = false)
            }
        }

        fun removeLine(productId: String) {
            _state.update { it.copy(lines = it.lines.filterNot { line -> line.productId == productId }) }
        }

        fun setInvoiceRequested(requested: Boolean) {
            _state.update { it.copy(invoiceRequested = requested, error = false) }
        }

        fun onBuyerNameChange(name: String) {
            _state.update { it.copy(buyerName = name, error = false) }
        }

        fun onBuyerNipChange(nip: String) {
            _state.update { it.copy(buyerNip = nip.filter(Char::isDigit).take(NIP_LENGTH), error = false) }
        }

        fun toggleAttributeValue(valueId: Int) {
            _state.update { current ->
                val selected = current.selectedAttributeValueIds
                current.copy(
                    selectedAttributeValueIds = if (valueId in selected) selected - valueId else selected + valueId,
                )
            }
        }

        fun checkout() {
            val current = _state.value
            if (!current.canCheckout) return

            _state.update { it.copy(submitting = true, error = false) }
            viewModelScope.launch {
                val placeId = authRepository.posSession.first()?.placeId
                if (placeId == null) {
                    _state.update { it.copy(submitting = false, error = true) }
                    return@launch
                }

                val invoiceDetails =
                    if (current.invoiceRequested) InvoiceDetails(current.buyerName, current.buyerNip) else null
                val options =
                    CheckoutOptions(
                        paymentMethod = PaymentMethod.CASH,
                        invoiceDetails = invoiceDetails,
                        attributeValueIds = current.selectedAttributeValueIds.toList(),
                    )

                salesRepository.checkout(orderId = orderId, placeId = placeId, lines = current.lines, options = options)
                syncQueue.releaseTable(tableId, orderId)
                _state.update { it.copy(submitting = false, queuedForSync = true) }
            }
        }
    }
