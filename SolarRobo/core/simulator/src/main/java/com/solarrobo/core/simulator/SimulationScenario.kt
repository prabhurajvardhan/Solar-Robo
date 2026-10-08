package com.solarrobo.core.simulator

/**
 * Standard deterministic testing scenarios for Solar Robo hardware and environment simulation.
 */
enum class SimulationScenario {
    NORMAL,
    LOW_LIGHT,
    HIGH_WIND,
    RAIN,
    MOTOR_JAM,
    BATTERY_LOW,
    DEVICE_OFFLINE
}
