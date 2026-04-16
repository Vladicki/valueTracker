package com.griffith.valuetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AppAccent,
    onPrimary = AppSurface,
    primaryContainer = AppAccent,
    onPrimaryContainer = AppSurface,
    secondary = AppTextSecondary,
    onSecondary = AppSurface,
    secondaryContainer = AppSurfaceMuted,
    onSecondaryContainer = AppTextPrimary,
    background = AppBackground,
    onBackground = AppTextPrimary,
    surface = AppSurface,
    onSurface = AppTextPrimary,
    surfaceVariant = AppSurfaceMuted,
    onSurfaceVariant = AppTextSecondary,
    outline = AppStroke,
)

@Composable
fun ValueTrackerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
