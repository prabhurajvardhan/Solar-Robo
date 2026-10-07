package com.solarrobo.core.contracts

data class EnvironmentSnapshot(
    val temperatureC: Float,
    val humidityPercent: Float,
    val lightLux: Float,
    val windSpeedMps: Float,
    val rainDetected: Boolean,
    val panelTemperatureC: Float,
    val timestamp: Long
)
