package pl.foodhub.pos.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette carried over from foodhub-app's styles/tokens.css so the terminal reads as
// the same product as the web panel: accent #ff7a18, info #2f80ed, success #169b8a,
// ink #13263a. Web's real primary-CTA color is accent orange (its "--color-primary"
// alias actually points at the info blue, but every .primary-button renders in
// accent) — mapped here to MaterialTheme's `primary` role directly instead of
// carrying the same alias-vs-reality mismatch into Compose.
private val Accent = Color(0xFFFF7A18)
private val AccentStrong = Color(0xFFD95B00)
private val Info = Color(0xFF2F80ED)
private val Success = Color(0xFF169B8A)
private val Ink = Color(0xFF13263A)
private val InkMuted = Color(0xFF60758A)
private val Canvas = Color(0xFFEEF3F8)
private val Danger = Color(0xFFDC2626)
private val Line = Color(0x3813263A)

/** Web's warning tone (tokens.css --color-warning) — no matching M3 role, exposed directly. */
val FoodHubWarning = Color(0xFFF0BA4F)

private val LightColors =
    lightColorScheme(
        primary = Accent,
        onPrimary = Color.White,
        primaryContainer = AccentStrong,
        onPrimaryContainer = Color.White,
        secondary = Info,
        onSecondary = Color.White,
        tertiary = Success,
        onTertiary = Color.White,
        background = Canvas,
        onBackground = Ink,
        surface = Color.White,
        onSurface = Ink,
        onSurfaceVariant = InkMuted,
        outline = Line,
        outlineVariant = Line,
        error = Danger,
    )

private val DarkColors =
    darkColorScheme(
        primary = Accent,
        onPrimary = Color.White,
        primaryContainer = AccentStrong,
        onPrimaryContainer = Color.White,
        secondary = Info,
        onSecondary = Color.White,
        tertiary = Success,
        onTertiary = Color.White,
        background = Color(0xFF071726),
        onBackground = Color(0xFFE8EEF4),
        surface = Color(0xFF12314D),
        onSurface = Color(0xFFE8EEF4),
        onSurfaceVariant = Color(0xFFA9BBCC),
        outline = Color(0x38E8EEF4),
        outlineVariant = Color(0x38E8EEF4),
        error = Danger,
    )

@Composable
fun FoodHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FoodHubTypography,
        shapes = FoodHubShapes,
        content = content,
    )
}
