@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package pl.foodhub.pos.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import pl.foodhub.pos.core.designsystem.R

// Same typefaces as foodhub-app (self-hosted IBM Plex Sans / Sans Condensed, see
// src/styles/tokens.css): condensed for display/headline/title-large, regular Sans
// for everything else. Sans ships as a single variable font; each weight below binds
// its own wght axis instantiation rather than letting the system fake-bold a single
// static weight.
private fun plexSansWeight(weight: Int) = FontVariation.Settings(FontVariation.weight(weight))

private val PlexSans =
    FontFamily(
        Font(R.font.ibm_plex_sans, FontWeight.Normal, variationSettings = plexSansWeight(400)),
        Font(R.font.ibm_plex_sans, FontWeight.Medium, variationSettings = plexSansWeight(500)),
        Font(R.font.ibm_plex_sans, FontWeight.SemiBold, variationSettings = plexSansWeight(600)),
        Font(R.font.ibm_plex_sans, FontWeight.Bold, variationSettings = plexSansWeight(700)),
    )

private val PlexSansCondensed =
    FontFamily(
        Font(R.font.ibm_plex_sans_condensed_semibold, FontWeight.SemiBold),
        Font(R.font.ibm_plex_sans_condensed_bold, FontWeight.Bold),
    )

private val defaults = Typography()

val FoodHubTypography =
    Typography(
        displayLarge = defaults.displayLarge.copy(fontFamily = PlexSansCondensed, fontWeight = FontWeight.Bold),
        displayMedium = defaults.displayMedium.copy(fontFamily = PlexSansCondensed, fontWeight = FontWeight.Bold),
        displaySmall = defaults.displaySmall.copy(fontFamily = PlexSansCondensed, fontWeight = FontWeight.SemiBold),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = PlexSansCondensed, fontWeight = FontWeight.SemiBold),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = PlexSansCondensed, fontWeight = FontWeight.SemiBold),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = PlexSansCondensed, fontWeight = FontWeight.SemiBold),
        titleLarge = defaults.titleLarge.copy(fontFamily = PlexSansCondensed, fontWeight = FontWeight.SemiBold),
        titleMedium = defaults.titleMedium.copy(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold),
        titleSmall = defaults.titleSmall.copy(fontFamily = PlexSans, fontWeight = FontWeight.Medium),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = PlexSans),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = PlexSans),
        bodySmall = defaults.bodySmall.copy(fontFamily = PlexSans),
        labelLarge = defaults.labelLarge.copy(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold),
        labelMedium = defaults.labelMedium.copy(fontFamily = PlexSans, fontWeight = FontWeight.Medium),
        labelSmall = defaults.labelSmall.copy(fontFamily = PlexSans, fontWeight = FontWeight.Medium),
    )
