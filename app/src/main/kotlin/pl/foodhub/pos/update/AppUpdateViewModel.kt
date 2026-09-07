package pl.foodhub.pos.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface AppUpdateUiState {
    data object Idle : AppUpdateUiState

    data class Available(val versionCode: Int) : AppUpdateUiState

    data object Downloading : AppUpdateUiState

    data class ReadyToInstall(val apkFile: File) : AppUpdateUiState
}

/**
 * Drives the one-shot "is a newer POS build available" check (triggered once per
 * session by [pl.foodhub.pos.MainActivity] after login) and the subsequent
 * download-then-install flow started from [AppUpdateHost].
 */
@HiltViewModel
class AppUpdateViewModel
    @Inject
    constructor(
        private val repository: PosAppUpdateRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
        val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

        private var checkedThisSession = false

        fun checkForUpdateOnce() {
            if (checkedThisSession) return
            checkedThisSession = true
            viewModelScope.launch {
                repository.newerVersionAvailable()?.let { versionCode ->
                    _state.value = AppUpdateUiState.Available(versionCode)
                }
            }
        }

        fun startUpdate() {
            viewModelScope.launch {
                _state.value = AppUpdateUiState.Downloading
                _state.value =
                    repository.downloadApk()
                        ?.let { AppUpdateUiState.ReadyToInstall(it) }
                        ?: AppUpdateUiState.Idle
            }
        }

        fun dismiss() {
            _state.value = AppUpdateUiState.Idle
        }

        /** Called once [AppUpdateHost] has handed the APK off to the system installer. */
        fun installRequestLaunched() {
            _state.value = AppUpdateUiState.Idle
        }
    }
