package com.solarrobo.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SolarColorScheme = lightColorScheme(
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
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SolarColorScheme,
        typography = SolarTypography,
        content = content
    )
}
