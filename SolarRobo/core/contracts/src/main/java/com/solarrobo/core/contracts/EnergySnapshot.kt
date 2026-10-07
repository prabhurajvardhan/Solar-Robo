package com.solarrobo.core.contracts

enum class GridState {
    IMPORTING,
    EXPORT_READY,
    ISOLATED,
    UNKNOWN
}

data class EnergySnapshot(
    val generatedWatts: Float,
    val consumedWatts: Float,
    val batteryPercent: Float,
    val batteryPowerWatts: Float,
    val reservePercent: Float,
    val gridState: GridState,
    val timestamp: Long
)

data class EnergyHistoryPoint(
    val timestamp: Long,
    val generatedWh: Float,
    val consumedWh: Float,
    val peakWatts: Float,
    val solarEfficiency: Float
)
