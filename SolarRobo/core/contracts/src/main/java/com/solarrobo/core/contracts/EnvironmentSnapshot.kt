package com.solarrobo.core.contracts

/**
 * Atmospheric and environmental sensor readings collected at the panel.
 */
data class EnvironmentSnapshot(
    val temperatureC: Float,
    val humidityPercent: Float,
    val lightLux: Float,
    val windSpeedMps: Float,
    val rainDetected: Boolean,
    val panelTemperatureC: Float,
    val timestamp: Long
)
