package com.solarrobo.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    @Test
    fun astraLightAndDarkTextPairsMeetWcagAa() {
        val schemes = listOf("light" to LightScheme, "dark" to DarkScheme)
        schemes.forEach { (name, scheme) ->
            listOf(
                "onBackground/background" to (scheme.onBackground to scheme.background),
                "onSurface/surface" to (scheme.onSurface to scheme.surface),
                "onSurfaceVariant/surface" to (scheme.onSurfaceVariant to scheme.surface),
                "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
                "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
                "error/background" to (scheme.error to scheme.background)
            ).forEach { (label, colors) ->
                val measured = contrast(colors.first, colors.second)
                assertTrue("$name $label contrast was $measured:1", measured >= 4.5)
            }
        }
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val high = maxOf(luminance(foreground), luminance(background))
        val low = minOf(luminance(foreground), luminance(background))
        return (high + 0.05) / (low + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun linear(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }
}
