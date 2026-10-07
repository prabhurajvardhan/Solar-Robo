package com.solarrobo.core.contracts

/**
 * Grid connection status for hybrid inverter integration.
 */
enum class GridState {
    IMPORTING,
    EXPORT_READY,
    ISOLATED,
    UNKNOWN
}

/**
 * Real-time energy telemetry snapshot from onboard power sensors.
 */
data class EnergySnapshot(
    val generatedWatts: Float,
    val consumedWatts: Float,
    val batteryPercent: Float,
    val batteryPowerWatts: Float,
    val reservePercent: Float,
    val gridState: GridState,
    val timestamp: Long
)

/**
 * Hourly/Daily aggregated energy data point for historical analytics.
 */
data class EnergyHistoryPoint(
    val timestamp: Long,
    val generatedWh: Float,
    val consumedWh: Float,
    val peakWatts: Float,
    val solarEfficiency: Float
)
