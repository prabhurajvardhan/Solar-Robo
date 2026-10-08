package com.solarrobo.core.simulator

import com.solarrobo.core.contracts.DeviceHealth
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.EnvironmentSnapshot
import com.solarrobo.core.contracts.GridState
import com.solarrobo.core.contracts.HealthIssue
import com.solarrobo.core.contracts.HealthStatus
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.contracts.SafetyLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class SimulatorEngineImpl(
    initialScenario: SimulationScenario = SimulationScenario.NORMAL
) : SimulatorEngine {

    private val _scenario = MutableStateFlow(initialScenario)
    private val _simulatedTime = MutableStateFlow(System.currentTimeMillis())
    private val _safetyEvents = MutableStateFlow<List<SafetyEvent>>(emptyList())
    private var faultSequence = 1L

    override val currentScenario: Flow<SimulationScenario> = _scenario.asStateFlow()

    override fun observeRoboSnapshot(): Flow<RoboSnapshot> = _scenario.map { sc ->
        val timestamp = _simulatedTime.value
        when (sc) {
            SimulationScenario.NORMAL -> RoboSnapshot(
                deviceId = "SOLAR-ROBO-SIM-01",
                name = "Solar Robo Primary",
                connected = true,
                mode = RoboMode.NORMAL,
                panelAngleDeg = 45f,
                targetAngleDeg = 45f,
                generationWatts = 320f,
                batteryPercent = 94f,
                timestamp = timestamp
            )
            SimulationScenario.LOW_LIGHT -> RoboSnapshot(
                deviceId = "SOLAR-ROBO-SIM-01",
                name = "Solar Robo Primary",
                connected = true,
                mode = RoboMode.NORMAL,
                panelAngleDeg = 45f,
                targetAngleDeg = 45f,
                generationWatts = 42f,
                batteryPercent = 91f,
                timestamp = timestamp
            )
            SimulationScenario.HIGH_WIND -> RoboSnapshot(
                deviceId = "SOLAR-ROBO-SIM-01",
                name = "Solar Robo Primary",
                connected = true,
                mode = RoboMode.SAFE,
                panelAngleDeg = 0f,
                targetAngleDeg = 0f,
                generationWatts = 180f,
                batteryPercent = 88f,
                timestamp = timestamp
            )
            SimulationScenario.RAIN -> RoboSnapshot(
                deviceId = "SOLAR-ROBO-SIM-01",
                name = "Solar Robo Primary",
                connected = true,
                mode = RoboMode.PROTECTING,
                panelAngleDeg = 15f,
                targetAngleDeg = 15f,
                generationWatts = 60f,
                batteryPercent = 85f,
                timestamp = timestamp
            )
            SimulationScenario.MOTOR_JAM -> RoboSnapshot(
                deviceId = "SOLAR-ROBO-SIM-01",
                name = "Solar Robo Primary",
                connected = true,
                mode = RoboMode.FAULT,
                panelAngleDeg = 32f,
                targetAngleDeg = 45f,
                generationWatts = 210f,
                batteryPercent = 82f,
                timestamp = timestamp
            )
            SimulationScenario.BATTERY_LOW -> RoboSnapshot(
                deviceId = "SOLAR-ROBO-SIM-01",
                name = "Solar Robo Primary",
                connected = true,
                mode = RoboMode.CONSERVING,
                panelAngleDeg = 45f,
                targetAngleDeg = 45f,
                generationWatts = 120f,
                batteryPercent = 8f,
                timestamp = timestamp
            )
            SimulationScenario.DEVICE_OFFLINE -> RoboSnapshot(
                deviceId = "SOLAR-ROBO-SIM-01",
                name = "Solar Robo Primary",
                connected = false,
                mode = RoboMode.FAULT,
                panelAngleDeg = 45f,
                targetAngleDeg = 45f,
                generationWatts = 0f,
                batteryPercent = 0f,
                timestamp = timestamp
            )
        }
    }

    override fun observeEnergySnapshot(): Flow<EnergySnapshot> = _scenario.map { sc ->
        val timestamp = _simulatedTime.value
        val (gen, bat, grid) = when (sc) {
            SimulationScenario.NORMAL -> Triple(320f, 94f, GridState.EXPORT_READY)
            SimulationScenario.LOW_LIGHT -> Triple(42f, 91f, GridState.IMPORTING)
            SimulationScenario.HIGH_WIND -> Triple(180f, 88f, GridState.EXPORT_READY)
            SimulationScenario.RAIN -> Triple(60f, 85f, GridState.IMPORTING)
            SimulationScenario.MOTOR_JAM -> Triple(210f, 82f, GridState.EXPORT_READY)
            SimulationScenario.BATTERY_LOW -> Triple(120f, 8f, GridState.IMPORTING)
            SimulationScenario.DEVICE_OFFLINE -> Triple(0f, 0f, GridState.UNKNOWN)
        }
        EnergySnapshot(
            generatedWatts = gen,
            consumedWatts = 80f,
            batteryPercent = bat,
            batteryPowerWatts = gen - 80f,
            reservePercent = 20f,
            gridState = grid,
            timestamp = timestamp
        )
    }

    override fun observeEnvironmentSnapshot(): Flow<EnvironmentSnapshot> = _scenario.map { sc ->
        val timestamp = _simulatedTime.value
        when (sc) {
            SimulationScenario.NORMAL -> EnvironmentSnapshot(
                temperatureC = 25f,
                humidityPercent = 40f,
                lightLux = 85000f,
                windSpeedMps = 3.5f,
                rainDetected = false,
                panelTemperatureC = 38f,
                timestamp = timestamp
            )
            SimulationScenario.LOW_LIGHT -> EnvironmentSnapshot(
                temperatureC = 19f,
                humidityPercent = 70f,
                lightLux = 8500f,
                windSpeedMps = 2.0f,
                rainDetected = false,
                panelTemperatureC = 22f,
                timestamp = timestamp
            )
            SimulationScenario.HIGH_WIND -> EnvironmentSnapshot(
                temperatureC = 22f,
                humidityPercent = 50f,
                lightLux = 65000f,
                windSpeedMps = 18.5f,
                rainDetected = false,
                panelTemperatureC = 30f,
                timestamp = timestamp
            )
            SimulationScenario.RAIN -> EnvironmentSnapshot(
                temperatureC = 17f,
                humidityPercent = 95f,
                lightLux = 12000f,
                windSpeedMps = 8.0f,
                rainDetected = true,
                panelTemperatureC = 19f,
                timestamp = timestamp
            )
            SimulationScenario.MOTOR_JAM -> EnvironmentSnapshot(
                temperatureC = 26f,
                humidityPercent = 42f,
                lightLux = 80000f,
                windSpeedMps = 4.0f,
                rainDetected = false,
                panelTemperatureC = 44f,
                timestamp = timestamp
            )
            SimulationScenario.BATTERY_LOW -> EnvironmentSnapshot(
                temperatureC = 24f,
                humidityPercent = 45f,
                lightLux = 75000f,
                windSpeedMps = 3.0f,
                rainDetected = false,
                panelTemperatureC = 35f,
                timestamp = timestamp
            )
            SimulationScenario.DEVICE_OFFLINE -> EnvironmentSnapshot(
                temperatureC = 0f,
                humidityPercent = 0f,
                lightLux = 0f,
                windSpeedMps = 0f,
                rainDetected = false,
                panelTemperatureC = 0f,
                timestamp = timestamp
            )
        }
    }

    override fun observeDeviceHealth(): Flow<DeviceHealth> = _scenario.map { sc ->
        val timestamp = _simulatedTime.value
        val offline = sc == SimulationScenario.DEVICE_OFFLINE
        val jam = sc == SimulationScenario.MOTOR_JAM
        val overall = when {
            offline -> HealthStatus.OFFLINE
            jam -> HealthStatus.CRITICAL
            sc == SimulationScenario.BATTERY_LOW -> HealthStatus.DEGRADED
            else -> HealthStatus.HEALTHY
        }
        val issues = when {
            jam -> listOf(
                HealthIssue(
                    subsystem = "motor",
                    issue = "Azimuth axis mechanical stall detected",
                    severity = HealthStatus.CRITICAL,
                    suggestedAction = "Inspect panel rotation assembly"
                )
            )
            offline -> listOf(
                HealthIssue(
                    subsystem = "comms",
                    issue = "Robo unit disconnected",
                    severity = HealthStatus.OFFLINE,
                    suggestedAction = "Verify power and antenna"
                )
            )
            else -> emptyList()
        }
        DeviceHealth(
            overallStatus = overall,
            motorStatus = if (jam) HealthStatus.CRITICAL else if (offline) HealthStatus.OFFLINE else HealthStatus.HEALTHY,
            batteryHealthPercent = if (sc == SimulationScenario.BATTERY_LOW) 65f else 98f,
            solarPanelStatus = if (offline) HealthStatus.OFFLINE else HealthStatus.HEALTHY,
            sensorsStatus = if (offline) HealthStatus.OFFLINE else HealthStatus.HEALTHY,
            cameraStatus = if (offline) HealthStatus.OFFLINE else HealthStatus.HEALTHY,
            controllerTemperatureC = if (offline) 0f else 34f,
            issues = issues,
            timestamp = timestamp
        )
    }

    override fun observeSafetyEvents(): Flow<List<SafetyEvent>> = _safetyEvents.asStateFlow()

    override suspend fun setScenario(scenario: SimulationScenario) {
        _scenario.value = scenario
        if (scenario == SimulationScenario.HIGH_WIND) {
            injectFault("SAFE_WIND_01", "High wind detected (>15m/s). Panel auto-stowed to neutral 0°.")
        } else if (scenario == SimulationScenario.MOTOR_JAM) {
            injectFault("MOTOR_STALL_01", "Mechanical stall alert on azimuth gear train.")
        }
    }

    override suspend fun injectFault(code: String, message: String): SafetyEvent {
        val event = SafetyEvent(
            id = "EVT-${faultSequence++}",
            level = SafetyLevel.FAULT,
            code = code,
            message = message,
            createdAt = _simulatedTime.value
        )
        _safetyEvents.value = _safetyEvents.value + event
        return event
    }

    override suspend fun tick(deltaMillis: Long) {
        _simulatedTime.value += deltaMillis
    }
}
