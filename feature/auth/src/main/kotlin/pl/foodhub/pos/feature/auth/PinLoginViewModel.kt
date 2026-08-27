package pl.foodhub.pos.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.foodhub.pos.core.auth.AuthRepository
import pl.foodhub.pos.core.common.ApiResult
import javax.inject.Inject

data class PinLoginUiState(
    val pin: String = "",
    val submitting: Boolean = false,
    val error: PinLoginError? = null,
    val loggedIn: Boolean = false,
) {
    val canSubmit: Boolean get() = pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && !submitting

    companion object {
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 6
    }
}

enum class PinLoginError {
    INVALID_PIN,
    OFFLINE,
    SERVER,
}

@HiltViewModel
class PinLoginViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(PinLoginUiState())
        val state = _state.asStateFlow()

        fun onPinChange(pin: String) {
            _state.update { it.copy(pin = pin.filter(Char::isDigit), error = null) }
        }

        fun submit() {
            val current = _state.value
            if (!current.canSubmit) return

            _state.update { it.copy(submitting = true, error = null) }
            viewModelScope.launch {
                when (val result = authRepository.loginWithPin(current.pin)) {
                    is ApiResult.Success -> _state.update { it.copy(submitting = false, loggedIn = true) }
                    is ApiResult.HttpError ->
                        _state.update {
                            it.copy(
                                submitting = false,
                                pin = "",
                                error = if (result.status == 401) PinLoginError.INVALID_PIN else PinLoginError.SERVER,
                            )
                        }
                    is ApiResult.NetworkError ->
                        _state.update { it.copy(submitting = false, error = PinLoginError.OFFLINE) }
                }
            }
        }
    }
