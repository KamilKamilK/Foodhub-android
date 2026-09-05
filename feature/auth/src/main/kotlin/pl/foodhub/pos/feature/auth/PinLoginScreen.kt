package pl.foodhub.pos.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.foodhub.pos.core.designsystem.component.PinPad
import pl.foodhub.pos.core.designsystem.component.PrimaryButton

@Composable
fun PinLoginRoute(
    onLoggedIn: () -> Unit,
    viewModel: PinLoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoggedIn()
    }

    PinLoginScreen(
        state = state,
        onPinChange = viewModel::onPinChange,
        onSubmit = viewModel::submit,
    )
}

@Composable
internal fun PinLoginScreen(
    state: PinLoginUiState,
    onPinChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "LOGOWANIE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "FoodHub Atelier",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Zaloguj się PIN-em",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PinPad(pin = state.pin, onPinChange = onPinChange, maxLength = PinLoginUiState.MAX_PIN_LENGTH)

            state.error?.let { error ->
                Text(
                    text =
                        when (error) {
                            PinLoginError.INVALID_PIN -> "Nieprawidłowy PIN."
                            PinLoginError.OFFLINE -> "Brak połączenia z serwerem."
                            PinLoginError.SERVER -> "Błąd serwera. Spróbuj ponownie."
                        },
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            if (state.submitting) {
                CircularProgressIndicator()
            } else {
                PrimaryButton(text = "Zaloguj", onClick = onSubmit, enabled = state.canSubmit)
            }
        }
    }
}
