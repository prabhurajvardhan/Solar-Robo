package com.solarrobo.feature.safety.data

import com.solarrobo.core.contracts.CommandResult
import com.solarrobo.core.contracts.HealthStatus
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.SafetyDecision
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.core.device.RoboDevice
import com.solarrobo.feature.safety.domain.SafetyContext
import com.solarrobo.feature.safety.domain.SafetyPolicy
import com.solarrobo.feature.safety.domain.SafetyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafetyRepositoryImpl @Inject constructor(
    private val device: RoboDevice,
    private val safetyPolicy: SafetyPolicy
) : SafetyRepository {

    private val events = MutableStateFlow<List<SafetyEvent>>(emptyList())

    override fun observeSafetyEvents(): Flow<List<SafetyEvent>> = events.asStateFlow()

    override suspend fun evaluateAndExecute(
        command: RoboCommand,
        context: SafetyContext
    ): SafetyDecision {
        val decision = safetyPolicy.evaluate(command, context)
        val dispatchCommand = when (decision) {
            SafetyDecision.Allow -> command
            is SafetyDecision.Block -> {
                val level = if (context.health.motorStatus == HealthStatus.CRITICAL) {
                    SafetyLevel.FAULT
                } else {
                    SafetyLevel.CAUTION
                }
                recordEvent(level, "COMMAND_BLOCKED", decision.reason)
                return decision
            }
            is SafetyDecision.Modify -> {
                recordEvent(SafetyLevel.PROTECTING, "COMMAND_MODIFIED", decision.reason)
                decision.command
            }
        }

        dispatch(dispatchCommand)
        return decision
    }

    override suspend fun emergencyStop(reason: String): CommandResult {
        recordEvent(SafetyLevel.EMERGENCY, "E_STOP", reason)
        return dispatch(RoboCommand.StopMotion)
    }

    override suspend fun safePosition(reason: String): CommandResult {
        recordEvent(SafetyLevel.PROTECTING, "SAFE_POSITION", reason)
        return dispatch(RoboCommand.SafePosition)
    }

    override suspend fun acknowledge(eventId: String) {
        events.update { current ->
            current.map { event ->
                if (event.id == eventId) event.copy(acknowledged = true) else event
            }
        }
    }

    private fun recordEvent(level: SafetyLevel, code: String, message: String) {
        val event = SafetyEvent(
            id = UUID.randomUUID().toString(),
            level = level,
            code = code,
            message = message,
            createdAt = System.currentTimeMillis()
        )
        events.update { it + event }
    }

    private suspend fun dispatch(command: RoboCommand): CommandResult {
        val result = try {
            device.sendCommand(command)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            recordEvent(
                SafetyLevel.FAULT,
                "COMMAND_DISPATCH_FAILED",
                exception.localizedMessage ?: "The device failed to execute the approved command."
            )
            throw exception
        }
        if (!result.accepted) {
            recordEvent(
                SafetyLevel.FAULT,
                "COMMAND_REJECTED",
                result.reason ?: "Device rejected the safety-approved command."
            )
        }
        return result
    }
}
