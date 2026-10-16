package com.jash.salescashtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

// iOS-inspired palette on top of Material 3
object AppColors {
    val Navy = Color(0xFF13294B)
    val Blue = Color(0xFF0A84FF)
    val Green = Color(0xFF34C759)
    val GreenText = Color(0xFF1A7F37)
    val Orange = Color(0xFFFF9500)
    val OrangeText = Color(0xFFB25E00)
    val Red = Color(0xFFFF3B30)
    val RedText = Color(0xFFC62828)
    val GroupedBg = Color(0xFFF2F2F7)
    val Card = Color(0xFFFFFFFF)
    val Separator = Color(0xFFE5E5EA)
    val Label = Color(0xFF1C1C1E)
    val Secondary = Color(0xFF8E8E93)
    val Fill = Color(0xFFEFEFF4)
}

private val colors = lightColorScheme(
    primary = AppColors.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F0FF),
    onPrimaryContainer = Color(0xFF00315F),
    secondary = AppColors.Navy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F0FF),
    onSecondaryContainer = AppColors.Navy,
    background = AppColors.GroupedBg,
    onBackground = AppColors.Label,
    surface = AppColors.GroupedBg,
    onSurface = AppColors.Label,
    surfaceVariant = AppColors.Fill,
    onSurfaceVariant = AppColors.Secondary,
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = AppColors.Fill,
    outline = Color(0xFFC7C7CC),
    outlineVariant = AppColors.Separator,
    error = AppColors.Red,
)

private val typography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp),
    bodyMedium = TextStyle(fontSize = 15.sp),
    bodySmall = TextStyle(fontSize = 13.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun SalesTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
