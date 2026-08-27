package pl.foodhub.pos.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette carried over from foodhub-app's styles/tokens.css so the terminal reads as
// the same product as the web panel: accent #ff7a18, info #2f80ed, success #169b8a,
// ink #13263a.
private val Accent = Color(0xFFFF7A18)
private val AccentStrong = Color(0xFFD95B00)
private val Info = Color(0xFF2F80ED)
private val Success = Color(0xFF169B8A)
private val Ink = Color(0xFF13263A)
private val Canvas = Color(0xFFEEF3F8)
private val Danger = Color(0xFFDC2626)

private val LightColors =
    lightColorScheme(
        primary = Info,
        onPrimary = Color.White,
        secondary = Accent,
        onSecondary = Color.White,
        tertiary = Success,
        background = Canvas,
        onBackground = Ink,
        surface = Color.White,
        onSurface = Ink,
        error = Danger,
    )

private val DarkColors =
    darkColorScheme(
        primary = Info,
        onPrimary = Color.White,
        secondary = Accent,
        onSecondary = Color.White,
        tertiary = Success,
        background = Color(0xFF071726),
        onBackground = Color(0xFFE8EEF4),
        surface = Color(0xFF12314D),
        onSurface = Color(0xFFE8EEF4),
        error = Danger,
    )

val FoodHubTypography = Typography()

@Composable
fun FoodHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FoodHubTypography,
        content = content,
    )
}
