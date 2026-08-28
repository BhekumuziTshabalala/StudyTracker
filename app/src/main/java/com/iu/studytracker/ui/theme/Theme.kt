package com.iu.studytracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

private val DarkColorScheme = darkColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0F3454),
    onPrimaryContainer = OceanBlueLight,

    secondary = SeafoamGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0A2E2B),
    onSecondaryContainer = SeafoamGreenLight,

    tertiary = SeafoamGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF0A2E2B),
    onTertiaryContainer = SeafoamGreenLight,

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    surfaceTint = OceanBlue,
    outline = DarkBorder,
    outlineVariant = Color(0xFF1E2D40),

    error = StatusRed,
    onError = Color.White,
    errorContainer = StatusRedContainer,
    onErrorContainer = Color(0xFFFCA5A5),
)

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = OceanBlueDark,

    secondary = SeafoamGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF0A3330),

    tertiary = SeafoamGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF0A3330),

    background = LightBackground,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,

    surfaceTint = OceanBlue,
    outline = LightBorder,
    outlineVariant = Color(0xFFDDE9F5),

    error = StatusRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
)

@Composable
fun DolphinPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = DolphinTypography,
        content = content
    )
}
