package com.solarrobo.feature.safety.presentation

import com.solarrobo.core.contracts.CommandResult
import com.solarrobo.core.contracts.SafetyDecision
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.contracts.SafetyLevel

data class SafetyUiState(
    val isLoading: Boolean = true,
    val currentLevel: SafetyLevel = SafetyLevel.NORMAL,
    val incidents: List<SafetyEvent> = emptyList(),
    val lastDecision: SafetyDecision? = null,
    val lastCommandResult: CommandResult? = null,
    val error: String? = null
) {
    val activeIncidents: List<SafetyEvent>
        get() = incidents.filterNot { it.acknowledged }

    val isEmergencyActive: Boolean
        get() = activeIncidents.any { it.level == SafetyLevel.EMERGENCY }
}
