package com.solarrobo.feature.safety.mock

import com.solarrobo.core.contracts.CommandResult
import com.solarrobo.core.contracts.DeviceHealth
import com.solarrobo.core.contracts.EnvironmentSnapshot
import com.solarrobo.core.contracts.HealthStatus
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.SafetyDecision
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.safety.domain.SafetyContext
import com.solarrobo.feature.safety.domain.SafetyPolicy
import com.solarrobo.feature.safety.domain.SafetyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeSafetyRepository(
    private val policy: SafetyPolicy = SafetyPolicy()
) : SafetyRepository {

    private val events = MutableStateFlow<List<SafetyEvent>>(emptyList())
    private val commands = mutableListOf<RoboCommand>()
    private var nextId = 1

    override fun observeSafetyEvents(): Flow<List<SafetyEvent>> = events.asStateFlow()

    override suspend fun evaluateAndExecute(
        command: RoboCommand,
        context: SafetyContext
    ): SafetyDecision {
        val decision = policy.evaluate(command, context)
        when (decision) {
            SafetyDecision.Allow -> commands += command
            is SafetyDecision.Block -> {
                val level = if (context.health.motorStatus == HealthStatus.CRITICAL) {
                    SafetyLevel.FAULT
                } else {
                    SafetyLevel.CAUTION
                }
                record(level, "COMMAND_BLOCKED", decision.reason)
            }
            is SafetyDecision.Modify -> {
                commands += decision.command
                record(SafetyLevel.PROTECTING, "COMMAND_MODIFIED", decision.reason)
            }
        }
        return decision
    }

    override suspend fun emergencyStop(reason: String): CommandResult {
        record(SafetyLevel.EMERGENCY, "E_STOP", reason)
        commands += RoboCommand.StopMotion
        return nextCommandResult()
    }

    override suspend fun safePosition(reason: String): CommandResult {
        record(SafetyLevel.PROTECTING, "SAFE_POSITION", reason)
        commands += RoboCommand.SafePosition
        return nextCommandResult()
    }

    override suspend fun acknowledge(eventId: String) {
        events.update { current ->
            current.map { event ->
                if (event.id == eventId) event.copy(acknowledged = true) else event
            }
        }
    }

    fun dispatchedCommands(): List<RoboCommand> = commands.toList()

    private fun record(level: SafetyLevel, code: String, message: String) {
        val id = nextId++
        val event = SafetyEvent(
            id = "mock-event-$id",
            level = level,
            code = code,
            message = message,
            createdAt = id.toLong()
        )
        events.update { it + event }
    }

    private fun nextCommandResult(): CommandResult {
        val id = nextId++
        return CommandResult(
            accepted = true,
            commandId = "mock-command-$id",
            timestamp = id.toLong()
        )
    }

    companion object {
        fun normalSafetyContext(): SafetyContext = SafetyContext(
            environment = EnvironmentSnapshot(
                temperatureC = 25f,
                humidityPercent = 50f,
                lightLux = 60_000f,
                windSpeedMps = 0f,
                rainDetected = false,
                panelTemperatureC = 35f,
                timestamp = 1L
            ),
            health = DeviceHealth(
                overallStatus = HealthStatus.HEALTHY,
                motorStatus = HealthStatus.HEALTHY,
                batteryHealthPercent = 90f,
                solarPanelStatus = HealthStatus.HEALTHY,
                sensorsStatus = HealthStatus.HEALTHY,
                cameraStatus = HealthStatus.HEALTHY,
                controllerTemperatureC = 30f,
                issues = emptyList(),
                timestamp = 1L
            )
        )
    }
}
