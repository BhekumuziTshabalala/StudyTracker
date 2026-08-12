package com.iu.studytracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple60,
    onPrimary = Color.White,
    primaryContainer = Purple20,
    onPrimaryContainer = Purple80,
    secondary = Cyan60,
    onSecondary = Color.Black,
    secondaryContainer = Cyan20,
    onSecondaryContainer = Cyan80,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = StatusRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Purple60,
    onPrimary = Color.White,
    primaryContainer = Purple80,
    onPrimaryContainer = Purple20,
    secondary = Cyan60,
    onSecondary = Color.White,
    secondaryContainer = Cyan80,
    onSecondaryContainer = Cyan20,
    background = Color(0xFFF9FAFB),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFE5E7EB),
    error = StatusRed,
    onError = Color.White
)

@Composable
fun StudyTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudyTypography,
        content = content
    )
}
