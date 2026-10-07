package com.solarrobo.core.contracts

enum class RoboMode {
    NORMAL,
    OPTIMIZING,
    CONSERVING,
    PROTECTING,
    FAULT,
    SAFE
}

data class RoboSnapshot(
    val deviceId: String,
    val name: String,
    val connected: Boolean,
    val mode: RoboMode,
    val panelAngleDeg: Float,
    val targetAngleDeg: Float,
    val generationWatts: Float,
    val batteryPercent: Float,
    val timestamp: Long
)
