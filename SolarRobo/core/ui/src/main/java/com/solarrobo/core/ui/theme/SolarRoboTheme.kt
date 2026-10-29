package com.solarrobo.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val LightScheme = lightColorScheme(
    primary = Olive, onPrimary = Color.White,
    primaryContainer = OliveTint, onPrimaryContainer = OliveInk,
    secondary = OliveMuted, onSecondary = Color.White,
    secondaryContainer = OliveMutedTint, onSecondaryContainer = OliveInk,
    tertiary = SunAmber, onTertiary = Color.White, tertiaryContainer = SunAmberTint,
    background = HalfWhite, onBackground = InkPrimary,
    surface = HalfWhiteRaised, onSurface = InkPrimary,
    surfaceVariant = HalfWhiteDeep, onSurfaceVariant = InkSecondary,
    outline = Outline, error = ErrorRed, errorContainer = ErrorTint
)
internal val DarkScheme = darkColorScheme(
    primary = DarkPrimary, onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryCont, onPrimaryContainer = OliveTint,
    background = DarkBg, onBackground = DarkOnSurface,
    surface = DarkSurfaceNew, onSurface = DarkOnSurface,
    surfaceVariant = DarkPrimaryCont, onSurfaceVariant = DarkOnSurfaceV,
    outline = Outline, error = Color(0xFFFFB4AB), errorContainer = Color(0xFF601410)
)

@Composable
fun SolarRoboTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    // Never use dynamic colour: the ASTRA palette stays the same on every phone.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = SolarRoboTypography,
        content = content
    )
}
