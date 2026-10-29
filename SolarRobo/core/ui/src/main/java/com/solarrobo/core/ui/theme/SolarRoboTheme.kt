package com.solarrobo.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val SolarLightScheme = lightColorScheme(
    primary = SolarAmberDark,
    onPrimary = Color.White,
    secondary = SolarEmerald,
    onSecondary = Color.White,
    tertiary = ForestInk,
    background = Porcelain,
    surface = Color(0xFFFFFEFA),
    onBackground = ForestInk,
    onSurface = ForestInk,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = Slate,
    error = Color(0xFF9D3328)
)

private val SolarDarkScheme = darkColorScheme(
    primary = SolarCitron,
    onPrimary = ForestInk,
    secondary = Sage,
    onSecondary = ForestInk,
    tertiary = SolarAmber,
    background = Color(0xFF101A18),
    surface = Color(0xFF172522),
    onBackground = Porcelain,
    onSurface = Porcelain,
    surfaceVariant = Color(0xFF243631),
    onSurfaceVariant = Color(0xFFD0DDD7),
    outline = Sage,
    error = Color(0xFFFFB4A8)
)

@Composable
fun SolarRoboTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) SolarDarkScheme else SolarLightScheme,
        typography = SolarTypography,
        content = content
    )
}
