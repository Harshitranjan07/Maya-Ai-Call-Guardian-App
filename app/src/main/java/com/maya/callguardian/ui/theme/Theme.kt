package com.maya.callguardian.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MayaDarkColorScheme = darkColorScheme(
    primary = VioletAccent,
    onPrimary = TextPrimary,
    primaryContainer = NavySurfaceElevated,
    onPrimaryContainer = CyanAccent,
    secondary = CyanAccent,
    onSecondary = NavyDark,
    secondaryContainer = NavySurfaceElevated,
    onSecondaryContainer = CyanLight,
    tertiary = VioletLight,
    background = NavyDark,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavySurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = NavyBorder,
    error = RiskCriticalRed,
    onError = TextPrimary
)

@Composable
fun MayaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MayaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
