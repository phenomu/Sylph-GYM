package com.phenom.gym.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ModernDarkScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = TextPrimary,
    secondary = AccentCyan,
    onSecondary = BgDark,
    tertiary = AccentPink,
    onTertiary = BgDark,
    background = BgDark,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgCardAlt,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = AccentOrange,
    onError = BgDark,
)

@Composable
fun CyberpunkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ModernDarkScheme,
        typography = Typography,
        content = content
    )
}
