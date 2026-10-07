package com.solarrobo.feature.simulator.data

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
import com.solarrobo.feature.simulator.domain.Scenario
import com.solarrobo.feature.simulator.domain.SimulatorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.roundToInt

@Singleton  
class SimulatorRepositoryImpl @Inject constructor() : SimulatorRepository {
    private val simulatorState = MutableStateFlow(stateFor(Scenario.SUNNY_NORMAL))
    private val safetyEvents = MutableStateFlow<List<SafetyEvent>>(emptyList())
    private var nextEventNumber = 1L

    override fun getRoboSnapshot(): Flow<RoboSnapshot> = simulatorState
        .map { buildSnapshots(it).robo }
        .distinctUntilChanged()

    override fun getEnergySnapshot(): Flow<EnergySnapshot> = simulatorState
        .map { buildSnapshots(it).energy }
        .distinctUntilChanged()

    override fun getEnvironmentSnapshot(): Flow<EnvironmentSnapshot> = simulatorState
        .map { buildSnapshots(it).environment }
        .distinctUntilChanged()

    override fun getDeviceHealth(): Flow<DeviceHealth> = simulatorState
        .map { buildSnapshots(it).health }
        .distinctUntilChanged()

    override fun getSafetyEvents(): Flow<List<SafetyEvent>> = safetyEvents.asStateFlow()

    override suspend fun applyScenario(scenario: Scenario) {
        simulatorState.value = stateFor(scenario)
        nextEventNumber = 1L
        safetyEvents.value = emptyList()
        if (scenario == Scenario.MOTOR_JAM) {
            recordFault("MOTOR_STALL_01", "Simulated motor stall.")
        }
    }

    override suspend fun tick(deltaMillis: Long) {
        require(deltaMillis >= 0L) { "Simulated elapsed time must not be negative." }
        if (deltaMillis == 0L) return

        simulatorState.value = simulatorState.value.let { current ->
            val elapsedMillis = if (deltaMillis > Long.MAX_VALUE - current.elapsedMillis) {
                Long.MAX_VALUE
            } else {
                current.elapsedMillis + deltaMillis
            }
            val mayTrack = current.connected && current.mode != RoboMode.FAULT
            val maxMovement = TRACKING_RATE_DEGREES_PER_SECOND * deltaMillis / 1_000f
            val angleDelta = current.targetAngleDeg - current.panelAngleDeg
            val panelAngle = if (mayTrack) {
                current.panelAngleDeg + angleDelta.coerceIn(-maxMovement, maxMovement)
            } else {
                current.panelAngleDeg
            }.coerceIn(MIN_PANEL_ANGLE_DEGREES, MAX_PANEL_ANGLE_DEGREES)
            val alignment = cos(Math.toRadians((current.targetAngleDeg - panelAngle).toDouble()))
                .coerceAtLeast(0.2)
            val generationWatts = (current.nominalGenerationWatts * alignment).roundToInt().toFloat()
            val netEnergyWh = (generationWatts - CONSUMED_WATTS) * deltaMillis / MILLIS_PER_HOUR
            val batteryPercent = (current.batteryPercent + netEnergyWh / BATTERY_CAPACITY_WH * 100f)
                .coerceIn(0f, 100f)

            current.copy(
                elapsedMillis = elapsedMillis,
                panelAngleDeg = panelAngle,
                generationWatts = generationWatts,
                batteryPercent = batteryPercent
            )
        }
    }

    override suspend fun injectFault(code: String, message: String): SafetyEvent {
        if (code == MOTOR_STALL_CODE) {
            simulatorState.value = simulatorState.value.copy(mode = RoboMode.FAULT)
        }
        return recordFault(code, message)
    }

    private fun recordFault(code: String, message: String): SafetyEvent {
        val event = SafetyEvent(
            id = "SIM-EVENT-${nextEventNumber.toString().padStart(6, '0')}",
            level = SafetyLevel.FAULT,
            code = code,
            message = message,
            createdAt = simulatorState.value.elapsedMillis
        )
        nextEventNumber += 1L
        safetyEvents.value = safetyEvents.value + event
        return event
    }

    private fun buildSnapshots(state: SimulatorState): SimulatorSnapshots {
        val timestamp = state.elapsedMillis
        val robo = RoboSnapshot(
            deviceId = SIMULATED_DEVICE_ID,
            name = SIMULATED_DEVICE_NAME,
            connected = state.connected,
            mode = state.mode,
            panelAngleDeg = state.panelAngleDeg,
            targetAngleDeg = state.targetAngleDeg,
            generationWatts = state.generationWatts,
            batteryPercent = state.batteryPercent,
            timestamp = timestamp
        )
        val environment = EnvironmentSnapshot(
            temperatureC = state.temperatureC,
            humidityPercent = state.humidityPercent,
            lightLux = state.lightLux,
            windSpeedMps = state.windSpeedMps,
            rainDetected = state.rainDetected,
            panelTemperatureC = state.panelTemperatureC,
            timestamp = timestamp
        )
        val energy = EnergySnapshot(
            generatedWatts = state.generationWatts,
            consumedWatts = CONSUMED_WATTS,
            batteryPercent = state.batteryPercent,
            batteryPowerWatts = state.generationWatts - CONSUMED_WATTS,
            reservePercent = BATTERY_RESERVE_PERCENT,
            gridState = when {
                !state.connected -> GridState.UNKNOWN
                state.generationWatts >= CONSUMED_WATTS -> GridState.EXPORT_READY
                else -> GridState.IMPORTING
            },
            timestamp = timestamp
        )
        val health = buildDeviceHealth(state, timestamp)
        return SimulatorSnapshots(robo, energy, environment, health)
    }

    private fun buildDeviceHealth(state: SimulatorState, timestamp: Long): DeviceHealth {
        val offline = !state.connected
        val motorStatus = when {
            offline -> HealthStatus.OFFLINE
            state.mode == RoboMode.FAULT -> HealthStatus.CRITICAL
            else -> HealthStatus.HEALTHY
        }
        val overallStatus = when {
            offline -> HealthStatus.OFFLINE
            motorStatus == HealthStatus.CRITICAL -> HealthStatus.CRITICAL
            else -> HealthStatus.HEALTHY
        }
        val issues = when {
            offline -> listOf(
                HealthIssue(
                    subsystem = "controller",
                    issue = "Simulated device is offline.",
                    severity = HealthStatus.OFFLINE,
                    suggestedAction = "Reconnect the simulated device."
                )
            )
            motorStatus == HealthStatus.CRITICAL -> listOf(
                HealthIssue(
                    subsystem = "motor",
                    issue = "Simulated motor stall.",
                    severity = HealthStatus.CRITICAL,
                    suggestedAction = "Inspect the simulated motor fault."
                )
            )
            else -> emptyList()
        }

        return DeviceHealth(
            overallStatus = overallStatus,
            motorStatus = motorStatus,
            batteryHealthPercent = 100f,
            solarPanelStatus = if (offline) HealthStatus.OFFLINE else HealthStatus.HEALTHY,
            sensorsStatus = if (offline) HealthStatus.OFFLINE else HealthStatus.HEALTHY,
            cameraStatus = if (offline) HealthStatus.OFFLINE else HealthStatus.HEALTHY,
            controllerTemperatureC = if (offline) 0f else state.temperatureC,
            issues = issues,
            timestamp = timestamp
        )
    }

    private fun stateFor(scenario: Scenario): SimulatorState = when (scenario) {
        Scenario.SUNNY_NORMAL -> SimulatorState(
            scenario = scenario,
            connected = true,
            mode = RoboMode.NORMAL,
            panelAngleDeg = 35f,
            targetAngleDeg = 45f,
            nominalGenerationWatts = 320f,
            generationWatts = 320f,
            batteryPercent = 92f,
            temperatureC = 26f,
            humidityPercent = 45f,
            lightLux = 85_000f,
            windSpeedMps = 3.5f,
            rainDetected = false,
            panelTemperatureC = 40f
        )
        Scenario.LOW_LIGHT -> SimulatorState(
            scenario = scenario,
            connected = true,
            mode = RoboMode.NORMAL,
            panelAngleDeg = 35f,
            targetAngleDeg = 45f,
            nominalGenerationWatts = 40f,
            generationWatts = 40f,
            batteryPercent = 92f,
            temperatureC = 26f,
            humidityPercent = 45f,
            lightLux = 5_000f,
            windSpeedMps = 3.5f,
            rainDetected = false,
            panelTemperatureC = 30f
        )
        Scenario.HIGH_WIND -> SimulatorState(
            scenario = scenario,
            connected = true,
            mode = RoboMode.NORMAL,
            panelAngleDeg = 35f,
            targetAngleDeg = 45f,
            nominalGenerationWatts = 320f,
            generationWatts = 320f,
            batteryPercent = 92f,
            temperatureC = 26f,
            humidityPercent = 45f,
            lightLux = 85_000f,
            windSpeedMps = 18.2f,
            rainDetected = false,
            panelTemperatureC = 40f
        )
        Scenario.RAIN -> SimulatorState(
            scenario = scenario,
            connected = true,
            mode = RoboMode.NORMAL,
            panelAngleDeg = 35f,
            targetAngleDeg = 45f,
            nominalGenerationWatts = 60f,
            generationWatts = 60f,
            batteryPercent = 92f,
            temperatureC = 20f,
            humidityPercent = 80f,
            lightLux = 15_000f,
            windSpeedMps = 3.5f,
            rainDetected = true,
            panelTemperatureC = 25f
        )
        Scenario.MOTOR_JAM -> SimulatorState(
            scenario = scenario,
            connected = true,
            mode = RoboMode.FAULT,
            panelAngleDeg = 35f,
            targetAngleDeg = 45f,
            nominalGenerationWatts = 320f,
            generationWatts = 320f,
            batteryPercent = 92f,
            temperatureC = 26f,
            humidityPercent = 45f,
            lightLux = 85_000f,
            windSpeedMps = 3.5f,
            rainDetected = false,
            panelTemperatureC = 40f
        )
        Scenario.BATTERY_LOW -> SimulatorState(
            scenario = scenario,
            connected = true,
            mode = RoboMode.CONSERVING,
            panelAngleDeg = 35f,
            targetAngleDeg = 45f,
            nominalGenerationWatts = 320f,
            generationWatts = 320f,
            batteryPercent = 8f,
            temperatureC = 26f,
            humidityPercent = 45f,
            lightLux = 85_000f,
            windSpeedMps = 3.5f,
            rainDetected = false,
            panelTemperatureC = 40f
        )
        Scenario.DEVICE_OFFLINE -> SimulatorState(
            scenario = scenario,
            connected = false,
            mode = RoboMode.NORMAL,
            panelAngleDeg = 35f,
            targetAngleDeg = 45f,
            nominalGenerationWatts = 320f,
            generationWatts = 320f,
            batteryPercent = 92f,
            temperatureC = 26f,
            humidityPercent = 45f,
            lightLux = 85_000f,
            windSpeedMps = 3.5f,
            rainDetected = false,
            panelTemperatureC = 40f
        )
    }

    private data class SimulatorState(
        val scenario: Scenario,
        val elapsedMillis: Long = 0L,
        val connected: Boolean,
        val mode: RoboMode,
        val panelAngleDeg: Float,
        val targetAngleDeg: Float,
        val nominalGenerationWatts: Float,
        val generationWatts: Float,
        val batteryPercent: Float,
        val temperatureC: Float,
        val humidityPercent: Float,
        val lightLux: Float,
        val windSpeedMps: Float,
        val rainDetected: Boolean,
        val panelTemperatureC: Float
    )

    private data class SimulatorSnapshots(
        val robo: RoboSnapshot,
        val energy: EnergySnapshot,
        val environment: EnvironmentSnapshot,
        val health: DeviceHealth
    )

    private companion object {
        const val SIMULATED_DEVICE_ID = "SIM-01"
        const val SIMULATED_DEVICE_NAME = "Simulated Robo"
        const val MOTOR_STALL_CODE = "MOTOR_STALL_01"
        const val CONSUMED_WATTS = 45f
        const val BATTERY_RESERVE_PERCENT = 20f
        const val BATTERY_CAPACITY_WH = 1_000f
        const val TRACKING_RATE_DEGREES_PER_SECOND = 0.25f
        const val MIN_PANEL_ANGLE_DEGREES = -90f
        const val MAX_PANEL_ANGLE_DEGREES = 90f
        const val MILLIS_PER_HOUR = 3_600_000f
    }
}