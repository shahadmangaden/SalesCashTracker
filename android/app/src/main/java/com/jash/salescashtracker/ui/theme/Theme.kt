package com.jash.salescashtracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import com.jash.salescashtracker.ui.Glass
import com.jash.salescashtracker.ui.Palette
import com.jash.salescashtracker.ui.ThemeState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.jash.salescashtracker.R

/** Outfit, the typeface used in the theme mockups. */
val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold),
)

/** Older name kept so every screen picks up the new typeface. */
val Inter = Outfit

// Older palette names still referenced by shared form components; they follow the active theme.
object AppColors {
    private val p get() = ThemeState.palette
    val Navy get() = p.ink
    val Blue get() = p.primary
    val Green get() = Glass.Green
    val GreenText get() = Glass.Green
    val Orange get() = Glass.Amber
    val OrangeText get() = Glass.Amber
    val Red get() = Glass.Red
    val RedText get() = Glass.Red
    val GroupedBg get() = p.bg.first()
    val Card get() = p.card
    val Separator get() = p.divider
    val Label get() = p.ink
    val Secondary get() = p.muted
    val Fill get() = p.tile
}

private fun schemeFor(p: Palette) = (if (p.darkCard) darkColorScheme() else lightColorScheme()).copy(
    primary = p.primary,
    onPrimary = p.onPrimary,
    primaryContainer = p.accentSoft,
    onPrimaryContainer = p.ink,
    secondary = p.accent,
    onSecondary = p.onAccent,
    secondaryContainer = p.accentSoft,
    onSecondaryContainer = p.ink,
    tertiary = p.accent,
    onTertiary = p.onAccent,
    background = p.bg.first(),
    onBackground = p.onBg,
    surface = p.card,
    onSurface = p.ink,
    surfaceVariant = p.tile,
    onSurfaceVariant = p.muted,
    surfaceContainerLowest = p.card,
    surfaceContainer = p.card,
    surfaceContainerLow = p.card,
    surfaceContainerHigh = p.card,
    surfaceContainerHighest = p.tile,
    surfaceBright = p.card,
    surfaceDim = p.tile,
    inverseSurface = p.ink,
    inverseOnSurface = p.card,
    outline = p.divider,
    outlineVariant = p.divider,
    error = Glass.Red,
)

private fun s(size: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Outfit, fontSize = size.sp, fontWeight = weight, lineHeight = 1.3.em)

private val typography = Typography(
    displayLarge = s(38, FontWeight.Bold),
    displayMedium = s(36, FontWeight.Bold),
    displaySmall = s(32, FontWeight.Bold),
    headlineLarge = s(30, FontWeight.Bold),
    headlineMedium = s(26, FontWeight.Bold),
    headlineSmall = s(22, FontWeight.Bold),
    titleLarge = s(18, FontWeight.Bold),
    titleMedium = s(16, FontWeight.SemiBold),
    titleSmall = s(14, FontWeight.SemiBold),
    bodyLarge = s(14),
    bodyMedium = s(13),
    bodySmall = s(12),
    labelLarge = s(14, FontWeight.SemiBold),
    labelMedium = s(12, FontWeight.Medium),
    labelSmall = s(11, FontWeight.Medium),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(15.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun SalesTheme(content: @Composable () -> Unit) {
    // Respect the phone's text size, but cap very large settings so cards and numbers don't get cut off.
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale.coerceAtMost(1.15f))) {
        MaterialTheme(colorScheme = schemeFor(ThemeState.palette), typography = typography, shapes = shapes, content = content)
    }
}
