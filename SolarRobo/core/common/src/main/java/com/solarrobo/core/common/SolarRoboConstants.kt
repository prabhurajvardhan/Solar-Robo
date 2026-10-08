package com.solarrobo.core.common

/**
 * Universal system constants for the Solar Robo hardware ecosystem.
 */
object SolarRoboConstants {
    const val MIN_PANEL_ANGLE_DEG: Float = -90.0f
    const val MAX_PANEL_ANGLE_DEG: Float = 90.0f
    const val SAFE_STOW_ANGLE_DEG: Float = 0.0f
    const val HIGH_WIND_THRESHOLD_MPS: Float = 15.0f
    const val BATTERY_CRITICAL_PERCENT: Float = 10.0f
    const val DEFAULT_DEVICE_NAME: String = "Solar Robo"
    const val TELEMETRY_POLL_INTERVAL_MS: Long = 1_000L
}
