package pl.foodhub.pos.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.foodhub.pos.core.common.ApiResult
import javax.inject.Inject

data class MenuBrowseUiState(
    val menu: Menu = Menu(emptyList(), emptyList()),
    val refreshing: Boolean = false,
    val stale: Boolean = false,
    val emptyOffline: Boolean = false,
)

@HiltViewModel
class MenuBrowseViewModel
    @Inject
    constructor(
        private val menuRepository: MenuRepository,
    ) : ViewModel() {
        private val staleFlag = MutableStateFlow(false)
        private val emptyOfflineFlag = MutableStateFlow(false)
        private val refreshing = MutableStateFlow(false)

        val state =
            combine(
                menuRepository.menu,
                refreshing,
                staleFlag,
                emptyOfflineFlag,
            ) { menu, isRefreshing, isStale, isEmptyOffline ->
                MenuBrowseUiState(
                    menu = menu,
                    refreshing = isRefreshing,
                    stale = isStale,
                    emptyOffline = isEmptyOffline,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuBrowseUiState())

        init {
            refresh()
        }

        fun refresh() {
            refreshing.value = true
            viewModelScope.launch {
                val result = menuRepository.refresh()
                val hasCache = menuRepository.hasCachedMenu()
                staleFlag.value = result !is ApiResult.Success && hasCache
                emptyOfflineFlag.value = result !is ApiResult.Success && !hasCache
                refreshing.value = false
            }
        }
    }
