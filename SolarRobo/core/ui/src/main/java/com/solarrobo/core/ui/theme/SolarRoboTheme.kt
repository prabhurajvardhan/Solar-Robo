package com.solarrobo.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SolarAmber,
    onPrimary = DarkBackground,
    secondary = SolarSky,
    onSecondary = DarkBackground,
    tertiary = SolarEmerald,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = SolarRose
)

@Composable
fun SolarRoboTheme(
    darkTheme: Boolean = true, // Solar Robo defaults to a focused dark tech theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = SolarTypography,
        content = content
    )
}
