package com.solarrobo.core.contracts

/**
 * Operating mode of the Solar Robo hardware tracker.
 */
enum class RoboMode {
    NORMAL,       // Normal automatic sun-tracking mode
    OPTIMIZING,   // Fine-tuning based on camera-derived observations
    CONSERVING,   // Low solar elevation or configured low-activity mode
    PROTECTING,   // Camera-jitter heuristic hold or safety event
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
    val generationWatts: Float?,
    val batteryPercent: Float?,
    val timestamp: Long
)
