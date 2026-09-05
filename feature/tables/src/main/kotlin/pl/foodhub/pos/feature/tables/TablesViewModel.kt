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
import pl.foodhub.pos.core.sync.SyncQueue
import java.util.UUID
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
    val stale: Boolean = false,
    val emptyOffline: Boolean = false,
)

/** A table opened for ordering: either just occupied, or already had an open order. */
data class TableSession(val orderId: String, val tableId: String)

/**
 * Reads the room layout and which tables have an open order, and opens one for
 * ordering: a free table's order-create + occupy goes through [SyncQueue] and returns
 * immediately (optimistic, ANDROID_POS_ARCHITECTURE.md section 9 point 2) instead of
 * waiting on the network; an already-occupied table is resumed by its existing order.
 * A losing occupy (another terminal grabbed the same table first, section 9 point 4)
 * doesn't surface here as an error -- the queue's worker reconciles it, and the next
 * [load] shows the table's real occupant.
 *
 * The table list itself is observed from [TablesRepository]'s Room cache, mirroring how
 * `feature:menu` serves the menu screen: a failed [load] falls back to the last-known
 * list with [TablesUiState.stale] set when a cache exists, or reports
 * [TablesUiState.emptyOffline] when there is none to fall back on.
 */
@HiltViewModel
class TablesViewModel
    @Inject
    constructor(
        private val tablesRepository: TablesRepository,
        private val authRepository: AuthRepository,
        private val syncQueue: SyncQueue,
    ) : ViewModel() {
        private val _state = MutableStateFlow(TablesUiState())
        val state = _state.asStateFlow()

        private val _openedTable = Channel<TableSession>(Channel.BUFFERED)
        val openedTable = _openedTable.receiveAsFlow()

        init {
            viewModelScope.launch {
                tablesRepository.tables.collect { tables ->
                    _state.update { it.copy(tables = tables) }
                }
            }
        }

        fun load() {
            _state.update { it.copy(loading = true, error = false) }
            viewModelScope.launch {
                val result = tablesRepository.refresh()
                if (result is ApiResult.Success) {
                    _state.update { it.copy(loading = false, stale = false, emptyOffline = false) }
                } else {
                    val hasCache = tablesRepository.hasCachedTables()
                    _state.update { it.copy(loading = false, stale = hasCache, emptyOffline = !hasCache) }
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

                val orderId = UUID.randomUUID().toString()
                syncQueue.createOrder(orderId, placeId)
                syncQueue.occupyTable(table.id, orderId)
                _openedTable.send(TableSession(orderId, table.id))
            }
        }
    }
