package pl.foodhub.pos.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Numeric keypad for terminal PIN entry. Digits are masked by default with a
 * show/hide toggle, mirroring the password field in foodhub-app's LoginView
 * (ANDROID_POS_ARCHITECTURE.md 2.1a).
 */
@Composable
fun PinPad(
    pin: String,
    onPinChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = 6,
) {
    var revealed by remember { mutableStateOf(false) }
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (revealed) pin else "•".repeat(pin.length),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { revealed = !revealed }) {
            Text(if (revealed) "Ukryj" else "Pokaż")
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(keys) { key ->
            when (key) {
                "" -> Text("")
                "⌫" ->
                    ElevatedButton(
                        onClick = { if (pin.isNotEmpty()) onPinChange(pin.dropLast(1)) },
                        modifier = Modifier.aspectRatio(1.6f),
                    ) {
                        Text("⌫", style = MaterialTheme.typography.headlineSmall)
                    }
                else ->
                    ElevatedButton(
                        onClick = { if (pin.length < maxLength) onPinChange(pin + key) },
                        modifier = Modifier.aspectRatio(1.6f),
                    ) {
                        Text(key, style = MaterialTheme.typography.headlineSmall)
                    }
            }
        }
    }
}
