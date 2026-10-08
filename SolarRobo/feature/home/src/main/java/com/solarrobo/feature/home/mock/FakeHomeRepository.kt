package com.solarrobo.feature.home.mock

import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.GridState
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.home.domain.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HomeScenario {
    NORMAL,
    OFFLINE,
    PROTECTING,
    FAULT,
    ERROR
}

class FakeHomeRepository(
    initialScenario: HomeScenario = HomeScenario.NORMAL
) : HomeRepository {

    private val _roboSnapshot = MutableStateFlow(createRoboSnapshotForScenario(initialScenario))
    private val _energySnapshot = MutableStateFlow(createEnergySnapshotForScenario(initialScenario))
    private val _safetyLevel = MutableStateFlow(createSafetyLevelForScenario(initialScenario))
    private val _safetyEvents = MutableStateFlow(createSafetyEventsForScenario(initialScenario))
    private val _roboMessage = MutableStateFlow(createRoboMessageForScenario(initialScenario))
    private val _errorMessage = MutableStateFlow(if (initialScenario == HomeScenario.ERROR) "Command center telemetry unavailable." else null)

    override fun getRoboSnapshot(): Flow<RoboSnapshot> = _roboSnapshot.asStateFlow()

    override fun getEnergySnapshot(): Flow<EnergySnapshot> = _energySnapshot.asStateFlow()

    override fun getSafetyLevel(): Flow<SafetyLevel> = _safetyLevel.asStateFlow()

    override fun getActiveSafetyEvents(): Flow<List<com.solarrobo.core.contracts.SafetyEvent>> = _safetyEvents.asStateFlow()

    override fun getLatestRoboMessage(): Flow<String> = _roboMessage.asStateFlow()

    override fun getErrorMessage(): Flow<String?> = _errorMessage.asStateFlow()

    override suspend fun refresh() {
        // Deterministic refresh
    }

    fun setScenario(scenario: HomeScenario) {
        _roboSnapshot.value = createRoboSnapshotForScenario(scenario)
        _energySnapshot.value = createEnergySnapshotForScenario(scenario)
        _safetyLevel.value = createSafetyLevelForScenario(scenario)
        _safetyEvents.value = createSafetyEventsForScenario(scenario)
        _roboMessage.value = createRoboMessageForScenario(scenario)
        _errorMessage.value = if (scenario == HomeScenario.ERROR) {
            "Command center telemetry unavailable."
        } else {
            null
        }
    }

    fun emitError(message: String?) {
        _errorMessage.value = message
    }

    companion object {
        const val BASE_TIMESTAMP = 1_700_000_000_000L

        fun createRoboSnapshotForScenario(scenario: HomeScenario): RoboSnapshot {
            return when (scenario) {
                HomeScenario.NORMAL -> RoboSnapshot(
                    deviceId = "ROBO-CORE-01",
                    name = "Solar Robo Unit",
                    connected = true,
                    mode = RoboMode.NORMAL,
                    panelAngleDeg = 45.0f,
                    targetAngleDeg = 45.0f,
                    generationWatts = 320.0f,
                    batteryPercent = 94.0f,
                    timestamp = BASE_TIMESTAMP
                )
                HomeScenario.OFFLINE -> RoboSnapshot(
                    deviceId = "ROBO-CORE-01",
                    name = "Solar Robo Unit",
                    connected = false,
                    mode = RoboMode.FAULT,
                    panelAngleDeg = 45.0f,
                    targetAngleDeg = 45.0f,
                    generationWatts = 0.0f,
                    batteryPercent = 0.0f,
                    timestamp = BASE_TIMESTAMP
                )
                HomeScenario.PROTECTING -> RoboSnapshot(
                    deviceId = "ROBO-CORE-01",
                    name = "Solar Robo Unit",
                    connected = true,
                    mode = RoboMode.PROTECTING,
                    panelAngleDeg = 0.0f,
                    targetAngleDeg = 0.0f,
                    generationWatts = 180.0f,
                    batteryPercent = 88.0f,
                    timestamp = BASE_TIMESTAMP
                )
                HomeScenario.FAULT -> RoboSnapshot(
                    deviceId = "ROBO-CORE-01",
                    name = "Solar Robo Unit",
                    connected = true,
                    mode = RoboMode.FAULT,
                    panelAngleDeg = 32.0f,
                    targetAngleDeg = 45.0f,
                    generationWatts = 210.0f,
                    batteryPercent = 82.0f,
                    timestamp = BASE_TIMESTAMP
                )
                HomeScenario.ERROR -> RoboSnapshot(
                    deviceId = "ROBO-CORE-01",
                    name = "Solar Robo Unit",
                    connected = false,
                    mode = RoboMode.FAULT,
                    panelAngleDeg = 0.0f,
                    targetAngleDeg = 0.0f,
                    generationWatts = 0.0f,
                    batteryPercent = 0.0f,
                    timestamp = BASE_TIMESTAMP
                )
            }
        }

        fun createEnergySnapshotForScenario(scenario: HomeScenario): EnergySnapshot {
            val gen = when (scenario) {
                HomeScenario.NORMAL -> 320.0f
                HomeScenario.OFFLINE -> 0.0f
                HomeScenario.PROTECTING -> 180.0f
                HomeScenario.FAULT -> 210.0f
                HomeScenario.ERROR -> 0.0f
            }
            val bat = when (scenario) {
                HomeScenario.NORMAL -> 94.0f
                HomeScenario.OFFLINE -> 0.0f
                HomeScenario.PROTECTING -> 88.0f
                HomeScenario.FAULT -> 82.0f
                HomeScenario.ERROR -> 0.0f
            }
            return EnergySnapshot(
                generatedWatts = gen,
                consumedWatts = 85.0f,
                batteryPercent = bat,
                batteryPowerWatts = gen - 85.0f,
                reservePercent = 20.0f,
                gridState = if (scenario == HomeScenario.OFFLINE || scenario == HomeScenario.ERROR) GridState.UNKNOWN else GridState.EXPORT_READY,
                timestamp = BASE_TIMESTAMP
            )
        }

        fun createSafetyLevelForScenario(scenario: HomeScenario): SafetyLevel {
            return when (scenario) {
                HomeScenario.NORMAL -> SafetyLevel.NORMAL
                HomeScenario.OFFLINE -> SafetyLevel.FAULT
                HomeScenario.PROTECTING -> SafetyLevel.PROTECTING
                HomeScenario.FAULT -> SafetyLevel.FAULT
                HomeScenario.ERROR -> SafetyLevel.FAULT
            }
        }

        fun createRoboMessageForScenario(scenario: HomeScenario): String {
            return when (scenario) {
                HomeScenario.NORMAL -> "Solar generation is stable."
                HomeScenario.OFFLINE -> "Robo unit disconnected."
                HomeScenario.PROTECTING -> "High wind detected. Actuator safely stowed."
                HomeScenario.FAULT -> "Motor stall detected on azimuth axis."
                HomeScenario.ERROR -> "Communication error."
            }
        }

        fun createSafetyEventsForScenario(scenario: HomeScenario): List<com.solarrobo.core.contracts.SafetyEvent> {
            return when (scenario) {
                HomeScenario.NORMAL -> emptyList()
                HomeScenario.OFFLINE -> listOf(
                    com.solarrobo.core.contracts.SafetyEvent(
                        id = "ALERT-001",
                        level = SafetyLevel.FAULT,
                        code = "COMM_LOST",
                        message = "Wireless link to tracker lost.",
                        createdAt = BASE_TIMESTAMP,
                        acknowledged = false
                    )
                )
                HomeScenario.PROTECTING -> listOf(
                    com.solarrobo.core.contracts.SafetyEvent(
                        id = "ALERT-002",
                        level = SafetyLevel.PROTECTING,
                        code = "WIND_STOW",
                        message = "High wind threshold exceeded. Panel locked in stow position.",
                        createdAt = BASE_TIMESTAMP,
                        acknowledged = false
                    )
                )
                HomeScenario.FAULT -> listOf(
                    com.solarrobo.core.contracts.SafetyEvent(
                        id = "ALERT-003",
                        level = SafetyLevel.FAULT,
                        code = "AZIMUTH_STALL",
                        message = "Mechanical resistance detected on horizontal axis.",
                        createdAt = BASE_TIMESTAMP,
                        acknowledged = false
                    )
                )
                HomeScenario.ERROR -> emptyList()
            }
        }
    }
}
