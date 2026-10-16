package com.jash.salescashtracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
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

/** Inter, as used throughout the Figma design. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

// Older palette names still referenced by shared form components.
object AppColors {
    val Navy = Color(0xFF16243D)
    val Blue = Color(0xFF246BFD)
    val Green = Color(0xFF167E65)
    val GreenText = Color(0xFF167E65)
    val Orange = Color(0xFFA26622)
    val OrangeText = Color(0xFFA26622)
    val Red = Color(0xFFBD5260)
    val RedText = Color(0xFFBD5260)
    val GroupedBg = Color(0xFFF4F7FC)
    val Card = Color(0xFFFFFFFF)
    val Separator = Color(0xFFD9E2EF)
    val Label = Color(0xFF16243D)
    val Secondary = Color(0xFF64728B)
    val Fill = Color(0xFFE8EEF8)
}

private val colors = lightColorScheme(
    primary = AppColors.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EEFF),
    onPrimaryContainer = AppColors.Navy,
    secondary = AppColors.Navy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6EEFF),
    onSecondaryContainer = AppColors.Navy,
    background = AppColors.GroupedBg,
    onBackground = AppColors.Label,
    surface = Color(0xFFF7FAFF),
    onSurface = AppColors.Label,
    surfaceVariant = AppColors.Fill,
    onSurfaceVariant = AppColors.Secondary,
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = AppColors.Fill,
    outline = Color(0xFFD9E2EF),
    outlineVariant = AppColors.Separator,
    error = AppColors.Red,
)

private fun s(size: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Inter, fontSize = size.sp, fontWeight = weight, lineHeight = 1.3.em)

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
        MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
    }
}
