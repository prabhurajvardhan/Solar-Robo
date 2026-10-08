package com.solarrobo.feature.energy.presentation

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.feature.energy.domain.EnergySummaryMetrics

sealed interface EnergyUiState {
    data object Loading : EnergyUiState

    data class Success(
        val snapshot: EnergySnapshot,
        val history: List<EnergyHistoryPoint>,
        val metrics: EnergySummaryMetrics,
        val isHistoryEmpty: Boolean = history.isEmpty()
    ) : EnergyUiState

    data class Empty(
        val message: String = "No energy telemetry available yet."
    ) : EnergyUiState

    data class Error(
        val message: String
    ) : EnergyUiState
}
