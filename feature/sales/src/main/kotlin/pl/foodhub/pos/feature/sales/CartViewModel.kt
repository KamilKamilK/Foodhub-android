package pl.foodhub.pos.feature.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.common.Money
import pl.foodhub.pos.core.database.MenuCacheDao
import javax.inject.Inject

data class PickerProduct(
    val productId: String,
    val name: String,
    val unitPriceGross: Money,
)

data class CartUiState(
    val lines: List<CartLine> = emptyList(),
    val submitting: Boolean = false,
    val completedDocumentId: String? = null,
    val error: Boolean = false,
) {
    val total: Money get() = lines.total()
}

@HiltViewModel
class CartViewModel
    @Inject
    constructor(
        private val salesRepository: SalesRepository,
        menuCacheDao: MenuCacheDao,
    ) : ViewModel() {
        private val _state = MutableStateFlow(CartUiState())
        val state = _state.asStateFlow()

        val availableProducts =
            menuCacheDao.observeItems()
                .map { items ->
                    items.map { PickerProduct(it.productId, it.productName, Money(it.unitPriceGrossMinor)) }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

        // TODO(D-later): placeId comes from the paired device's POS context, resolved at
        // login. Hard-coded here until core:auth exposes the session's place.
        fun checkout(
            placeId: String,
            paymentMethod: PaymentMethod,
        ) {
            val lines = _state.value.lines
            if (lines.isEmpty() || _state.value.submitting) return

            _state.update { it.copy(submitting = true, error = false) }
            viewModelScope.launch {
                when (val result = salesRepository.checkout(placeId, lines, paymentMethod)) {
                    is ApiResult.Success ->
                        _state.update { it.copy(submitting = false, completedDocumentId = result.value) }
                    else ->
                        _state.update { it.copy(submitting = false, error = true) }
                }
            }
        }
    }
