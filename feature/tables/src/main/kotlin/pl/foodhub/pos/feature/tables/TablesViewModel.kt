package pl.foodhub.pos.feature.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.foodhub.pos.core.auth.AuthRepository
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.network.api.SalesApi
import pl.foodhub.pos.core.network.api.TablesApi
import pl.foodhub.pos.core.network.apiCall
import pl.foodhub.pos.core.network.model.CreateOrderRequestDto
import javax.inject.Inject

data class TableRow(
    val id: String,
    val label: String,
    val seats: Int,
    val occupied: Boolean,
    val openOrderId: String?,
)

data class TablesUiState(
    val tables: List<TableRow> = emptyList(),
    val loading: Boolean = true,
    val error: Boolean = false,
)

/** A table opened for ordering: either just occupied, or already had an open order. */
data class TableSession(val orderId: String, val tableId: String)

/**
 * Faza 1: read the room layout and which tables have an open order, and open one for
 * ordering -- a free table gets a new order created and occupied in one step
 * (`POST /v1/order/orders` -> `POST /v1/tables/{tableId}/occupy/{orderId}`), an
 * already-occupied table is resumed by its existing order. Conflict-safe occupy for
 * two terminals racing the same table (versioned resources on the API side) is Faza 2
 * -- ANDROID_POS_ARCHITECTURE.md section 9 point 4.
 */
@HiltViewModel
class TablesViewModel
    @Inject
    constructor(
        private val tablesApi: TablesApi,
        private val salesApi: SalesApi,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(TablesUiState())
        val state = _state.asStateFlow()

        private val _openedTable = Channel<TableSession>(Channel.BUFFERED)
        val openedTable = _openedTable.receiveAsFlow()

        fun load() {
            _state.update { it.copy(loading = true, error = false) }
            viewModelScope.launch {
                val tables = apiCall { tablesApi.tables() }
                val occupied = apiCall { tablesApi.occupiedTables() }

                if (tables is ApiResult.Success && occupied is ApiResult.Success) {
                    val openByTable = occupied.value.associateBy({ it.tableId }, { it.orderId })
                    _state.update {
                        it.copy(
                            loading = false,
                            tables =
                                tables.value.map { table ->
                                    TableRow(
                                        id = table.id,
                                        label = table.name.ifBlank { "Stolik ${table.number}" },
                                        seats = table.seats,
                                        occupied = openByTable.containsKey(table.id),
                                        openOrderId = openByTable[table.id],
                                    )
                                },
                        )
                    }
                } else {
                    _state.update { it.copy(loading = false, error = true) }
                }
            }
        }

        fun selectTable(table: TableRow) {
            val existingOrderId = table.openOrderId
            if (existingOrderId != null) {
                viewModelScope.launch { _openedTable.send(TableSession(existingOrderId, table.id)) }
                return
            }

            viewModelScope.launch {
                val placeId = authRepository.posSession.first()?.placeId
                if (placeId == null) {
                    _state.update { it.copy(error = true) }
                    return@launch
                }

                when (val created = apiCall { salesApi.createOrder(CreateOrderRequestDto(placeId)) }) {
                    is ApiResult.Success -> {
                        val orderId = created.value.id
                        when (apiCall { tablesApi.occupy(table.id, orderId) }) {
                            is ApiResult.Success -> _openedTable.send(TableSession(orderId, table.id))
                            else -> _state.update { it.copy(error = true) }
                        }
                    }
                    else -> _state.update { it.copy(error = true) }
                }
            }
        }
    }
