package pl.foodhub.pos.feature.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.network.api.TablesApi
import pl.foodhub.pos.core.network.apiCall
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

/**
 * Faza 1: read the room layout and which tables have an open order. Conflict-safe
 * occupy/release across terminals (versioned resources on the API side) is Faza 2 --
 * ANDROID_POS_ARCHITECTURE.md section 9 point 4.
 */
@HiltViewModel
class TablesViewModel
    @Inject
    constructor(
        private val tablesApi: TablesApi,
    ) : ViewModel() {
        private val _state = MutableStateFlow(TablesUiState())
        val state = _state.asStateFlow()

        init {
            load()
        }

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
    }
