package com.solarrobo.feature.simulator.presentation

import com.solarrobo.core.contracts.DeviceHealth
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.EnvironmentSnapshot
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.feature.simulator.domain.Scenario

data class SimulatorUiState(
    val currentScenario: Scenario = Scenario.SUNNY_NORMAL,
    val robo: RoboSnapshot? = null,
    val energy: EnergySnapshot? = null,
    val environment: EnvironmentSnapshot? = null,
    val deviceHealth: DeviceHealth? = null,
    val safetyEvents: List<SafetyEvent> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val isOffline: Boolean
        get() = robo?.connected == false

    val isEmpty: Boolean
        get() = !isLoading && robo == null
}