package com.solarrobo.core.contracts

enum class HealthStatus {
    HEALTHY,
    DEGRADED,
    CRITICAL,
    OFFLINE
}

data class HealthIssue(
    val subsystem: String,
    val issue: String,
    val severity: HealthStatus,
    val suggestedAction: String
)

data class DeviceHealth(
    val overallStatus: HealthStatus,
    val motorStatus: HealthStatus,
    val batteryHealthPercent: Float,
    val solarPanelStatus: HealthStatus,
    val sensorsStatus: HealthStatus,
    val cameraStatus: HealthStatus,
    val controllerTemperatureC: Float,
    val issues: List<HealthIssue>,
    val timestamp: Long
)
