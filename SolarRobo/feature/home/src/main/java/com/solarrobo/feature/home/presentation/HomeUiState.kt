package com.solarrobo.feature.home.presentation

import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.contracts.SafetyLevel

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val robo: RoboSnapshot,
        val energy: EnergySnapshot,
        val safetyLevel: SafetyLevel,
        val activeAlerts: List<SafetyEvent> = emptyList(),
        val statusMessage: String
    ) : HomeUiState

    data class Empty(
        val message: String = "No command center telemetry available."
    ) : HomeUiState

    data class Error(
        val message: String
    ) : HomeUiState
}
