package com.solarrobo.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val LightScheme = lightColorScheme(
    primary = AstraPrimary,
    onPrimary = Color.White,
    primaryContainer = AstraMintStrong,
    onPrimaryContainer = AstraPrimaryDark,
    secondary = AstraSuccess,
    onSecondary = Color.White,
    secondaryContainer = AstraMint,
    onSecondaryContainer = AstraPrimaryDark,
    tertiary = AstraSun,
    onTertiary = AstraText,
    tertiaryContainer = Color(0xFFFFF4D6),
    background = AstraBackground,
    onBackground = AstraText,
    surface = AstraSurface,
    onSurface = AstraText,
    surfaceVariant = AstraMint,
    onSurfaceVariant = AstraTextSecondary,
    outline = AstraOutline,
    error = AstraDanger,
    errorContainer = AstraDangerContainer
)

internal val DarkScheme = darkColorScheme(
    primary = AstraDarkPrimary,
    onPrimary = AstraDarkBackground,
    primaryContainer = DarkPrimaryCont,
    onPrimaryContainer = AstraDarkText,
    secondary = Color(0xFF80C99B),
    onSecondary = AstraDarkBackground,
    secondaryContainer = Color(0xFF1C4930),
    onSecondaryContainer = AstraDarkText,
    tertiary = Color(0xFFFFD36A),
    onTertiary = AstraDarkBackground,
    background = AstraDarkBackground,
    onBackground = AstraDarkText,
    surface = AstraDarkSurface,
    onSurface = AstraDarkText,
    surfaceVariant = Color(0xFF193322),
    onSurfaceVariant = AstraDarkTextSecondary,
    outline = AstraDarkOutline,
    error = Color(0xFFFFB4A8),
    errorContainer = Color(0xFF5D2119)
)

@Composable
fun SolarRoboTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = SolarRoboTypography,
        shapes = AstraShapes,
        content = content
    )
}
