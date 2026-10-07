package com.solarrobo.feature.safety.domain

import com.solarrobo.core.contracts.CommandResult
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.SafetyDecision
import com.solarrobo.core.contracts.SafetyEvent
import kotlinx.coroutines.flow.Flow

interface SafetyRepository {
    fun observeSafetyEvents(): Flow<List<SafetyEvent>>
    suspend fun evaluateAndExecute(command: RoboCommand, context: SafetyContext): SafetyDecision
    suspend fun emergencyStop(reason: String): CommandResult
    suspend fun safePosition(reason: String): CommandResult
    suspend fun acknowledge(eventId: String)
}
