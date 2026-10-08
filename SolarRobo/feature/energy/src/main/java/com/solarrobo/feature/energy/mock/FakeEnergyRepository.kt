package com.solarrobo.feature.energy.mock

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.GridState
import com.solarrobo.feature.energy.domain.EnergyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EnergyScenario {
    NORMAL,
    LOW_GENERATION,
    HIGH_GENERATION,
    LOW_BATTERY,
    EMPTY_HISTORY,
    ERROR
}

class FakeEnergyRepository(
    initialScenario: EnergyScenario = EnergyScenario.NORMAL
) : EnergyRepository {

    private val _snapshot = MutableStateFlow(createSnapshotForScenario(initialScenario))
    private val _history = MutableStateFlow(createHistoryForScenario(initialScenario))
    private val _errorMessage = MutableStateFlow(if (initialScenario == EnergyScenario.ERROR) "Energy telemetry sensor unavailable." else null)

    override fun getEnergySnapshot(): Flow<EnergySnapshot> = _snapshot.asStateFlow()

    override fun getEnergyHistory(): Flow<List<EnergyHistoryPoint>> = _history.asStateFlow()

    override fun getErrorMessage(): Flow<String?> = _errorMessage.asStateFlow()

    override suspend fun refresh() {
        // Deterministic refresh
    }

    fun setScenario(scenario: EnergyScenario) {
        _snapshot.value = createSnapshotForScenario(scenario)
        _history.value = createHistoryForScenario(scenario)
        _errorMessage.value = if (scenario == EnergyScenario.ERROR) {
            "Energy telemetry sensor unavailable."
        } else {
            null
        }
    }

    fun emitError(message: String?) {
        _errorMessage.value = message
    }

    companion object {
        const val BASE_TIMESTAMP = 1_700_000_000_000L

        fun createSnapshotForScenario(scenario: EnergyScenario): EnergySnapshot {
            return when (scenario) {
                EnergyScenario.NORMAL -> EnergySnapshot(
                    generatedWatts = 420.0f,
                    consumedWatts = 85.0f,
                    batteryPercent = 78.0f,
                    batteryPowerWatts = 335.0f,
                    reservePercent = 20.0f,
                    gridState = GridState.EXPORT_READY,
                    timestamp = BASE_TIMESTAMP
                )
                EnergyScenario.LOW_GENERATION -> EnergySnapshot(
                    generatedWatts = 45.0f,
                    consumedWatts = 85.0f,
                    batteryPercent = 70.0f,
                    batteryPowerWatts = -40.0f,
                    reservePercent = 20.0f,
                    gridState = GridState.IMPORTING,
                    timestamp = BASE_TIMESTAMP
                )
                EnergyScenario.HIGH_GENERATION -> EnergySnapshot(
                    generatedWatts = 580.0f,
                    consumedWatts = 90.0f,
                    batteryPercent = 92.0f,
                    batteryPowerWatts = 490.0f,
                    reservePercent = 20.0f,
                    gridState = GridState.EXPORT_READY,
                    timestamp = BASE_TIMESTAMP
                )
                EnergyScenario.LOW_BATTERY -> EnergySnapshot(
                    generatedWatts = 110.0f,
                    consumedWatts = 95.0f,
                    batteryPercent = 12.0f,
                    batteryPowerWatts = 15.0f,
                    reservePercent = 20.0f,
                    gridState = GridState.IMPORTING,
                    timestamp = BASE_TIMESTAMP
                )
                EnergyScenario.EMPTY_HISTORY -> EnergySnapshot(
                    generatedWatts = 420.0f,
                    consumedWatts = 85.0f,
                    batteryPercent = 78.0f,
                    batteryPowerWatts = 335.0f,
                    reservePercent = 20.0f,
                    gridState = GridState.EXPORT_READY,
                    timestamp = BASE_TIMESTAMP
                )
                EnergyScenario.ERROR -> EnergySnapshot(
                    generatedWatts = 0.0f,
                    consumedWatts = 0.0f,
                    batteryPercent = 0.0f,
                    batteryPowerWatts = 0.0f,
                    reservePercent = 20.0f,
                    gridState = GridState.UNKNOWN,
                    timestamp = BASE_TIMESTAMP
                )
            }
        }

        fun createHistoryForScenario(scenario: EnergyScenario): List<EnergyHistoryPoint> {
            if (scenario == EnergyScenario.EMPTY_HISTORY || scenario == EnergyScenario.ERROR) {
                return emptyList()
            }
            return listOf(
                EnergyHistoryPoint(
                    timestamp = BASE_TIMESTAMP - 14_400_000L,
                    generatedWh = 180.0f,
                    consumedWh = 80.0f,
                    peakWatts = 210.0f,
                    solarEfficiency = 0.88f
                ),
                EnergyHistoryPoint(
                    timestamp = BASE_TIMESTAMP - 10_800_000L,
                    generatedWh = 320.0f,
                    consumedWh = 85.0f,
                    peakWatts = 380.0f,
                    solarEfficiency = 0.92f
                ),
                EnergyHistoryPoint(
                    timestamp = BASE_TIMESTAMP - 7_200_000L,
                    generatedWh = 450.0f,
                    consumedWh = 85.0f,
                    peakWatts = 490.0f,
                    solarEfficiency = 0.95f
                ),
                EnergyHistoryPoint(
                    timestamp = BASE_TIMESTAMP - 3_600_000L,
                    generatedWh = 420.0f,
                    consumedWh = 85.0f,
                    peakWatts = 440.0f,
                    solarEfficiency = 0.94f
                )
            )
        }
    }
}
