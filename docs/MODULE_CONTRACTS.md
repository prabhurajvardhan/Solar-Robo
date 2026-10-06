# Solar Robo — Canonical Cross-Module Contracts

This document contains the authoritative Kotlin data models, interfaces, sealed hierarchies, and types that govern all communication across module boundaries.

> **Contract Rule:** If two modules need to exchange data, add or reuse a shared contract in `:core:contracts`. Never solve the problem by importing another feature module’s internal store, service, or repository.

---

## 1. Robo State & Mode (`core/contracts/RoboSnapshot.kt`)

```kotlin
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
```

---

## 2. Energy Contracts (`core/contracts/EnergySnapshot.kt`)

```kotlin
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
```

---

## 3. Physical Commands & Execution Results (`core/contracts/RoboCommand.kt`)

```kotlin
package com.solarrobo.core.contracts

/**
 * Commands dispatched to adjust tracker position or invoke safety stow.
 */
sealed interface RoboCommand {
    /** Request solar tracker to rotate to a specific azimuth/elevation angle */
    data class MoveToAngle(val angleDeg: Float) : RoboCommand

    /** Immediately stop motor actuation */
    data object StopMotion : RoboCommand

    /** Move panel to flat, aerodynamically neutral stow position (0 deg) */
    data object SafePosition : RoboCommand
}

/**
 * Result emitted after a command is evaluated and dispatched.
 */
data class CommandResult(
    val accepted: Boolean,
    val commandId: String,
    val reason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
```

---

## 4. Safety & Protection Contracts (`core/contracts/SafetyContracts.kt`)

```kotlin
package com.solarrobo.core.contracts

/**
 * System safety criticality level.
 */
enum class SafetyLevel {
    NORMAL,
    CAUTION,
    PROTECTING,
    FAULT,
    EMERGENCY
}

/**
 * Safety audit and incident record.
 */
data class SafetyEvent(
    val id: String,
    val level: SafetyLevel,
    val code: String,
    val message: String,
    val createdAt: Long,
    val acknowledged: Boolean = false
)

/**
 * Deterministic decision emitted by the Safety Gate before hardware actuation.
 */
sealed interface SafetyDecision {
    data object Allow : SafetyDecision
    data class Block(val reason: String) : SafetyDecision
    data class Modify(val command: RoboCommand, val reason: String) : SafetyDecision
}
```

---

## 5. Environmental Sensor Snapshot (`core/contracts/EnvironmentSnapshot.kt`)

```kotlin
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
```

---

## 6. Activity & Robo Memory (`core/contracts/ActivityEvent.kt`)

```kotlin
package com.solarrobo.core.contracts

enum class ActivityType {
    MOVEMENT,
    ENERGY,
    SAFETY,
    SYSTEM,
    AI,
    RECOVERY
}

/**
 * Chronological ledger entry capturing state transitions, actions, and events.
 */
data class ActivityEvent(
    val id: String,
    val type: ActivityType,
    val title: String,
    val detail: String,
    val timestamp: Long
)
```

---

## 7. Device Health & Diagnostics (`core/contracts/DeviceHealth.kt`)

```kotlin
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
```

---

## 8. Notifications (`core/contracts/NotificationEvent.kt`)

```kotlin
package com.solarrobo.core.contracts

enum class NotificationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class NotificationEvent(
    val id: String,
    val title: String,
    val body: String,
    val priority: NotificationPriority,
    val timestamp: Long,
    val isRead: Boolean = false,
    val actionDeepLink: String? = null
)
```

---

## 9. Hardware & Core Interfaces (`core/device/` & `core/ai/`)

```kotlin
package com.solarrobo.core.device

import com.solarrobo.core.contracts.*
import kotlinx.coroutines.flow.Flow

/**
 * Hardware abstraction boundary for physical ESP32 or simulated tracker.
 */
interface RoboDevice {
    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun getSnapshot(): RoboSnapshot
    suspend fun sendCommand(command: RoboCommand): CommandResult
    fun observeSnapshot(): Flow<RoboSnapshot>
}
```

```kotlin
package com.solarrobo.core.ai

/**
 * Natural language reasoning request for Robo Talk.
 */
data class AiRequest(
    val message: String,
    val context: Map<String, Any?>
)

data class AiResponse(
    val text: String,
    val intent: String? = null,
    val confidence: Float? = null
)

/**
 * Abstract AI engine interface backing natural language conversations.
 */
interface AiEngine {
    suspend fun generate(request: AiRequest): AiResponse
}
```
