package com.solarrobo.core.contracts

/**
 * Operating mode of the Solar Robo hardware tracker.
 */
enum class RoboMode {
    NORMAL,       // Normal automatic sun-tracking mode
    OPTIMIZING,   // Fine-tuning angle to maximize light intensity lux
    CONSERVING,   // Low power mode (e.g. night-time or low battery)
    PROTECTING,   // Wind-stow or heavy rain protection mode
    FAULT,        // Mechanical or sensor failure detected
    SAFE          // Actuator parked in safe mechanical position (0° horizontal)
}

/**
 * Complete snapshot of the robotic tracker hardware status.
 */
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
