package com.solarrobo.feature.safety.domain

import com.solarrobo.core.contracts.DeviceHealth
import com.solarrobo.core.contracts.EnvironmentSnapshot
import com.solarrobo.core.contracts.HealthStatus
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.SafetyDecision
import javax.inject.Inject

data class SafetyContext(
    val environment: EnvironmentSnapshot,
    val health: DeviceHealth
)

class SafetyPolicy @Inject constructor() {
    companion object {
        const val MAX_SAFE_WIND_SPEED_MPS = 15.0f
        const val MIN_BATTERY_RESERVE_PERCENT = 10.0f
        const val MAX_SAFE_ANGLE_DEG = 85.0f
        const val MIN_SAFE_ANGLE_DEG = -85.0f
    }

    fun evaluate(command: RoboCommand, context: SafetyContext): SafetyDecision {
        if (context.environment.windSpeedMps > MAX_SAFE_WIND_SPEED_MPS) {
            return SafetyDecision.Modify(
                RoboCommand.SafePosition,
                "High wind speed (${context.environment.windSpeedMps} m/s) forces safe stow."
            )
        }

        if (context.health.motorStatus == HealthStatus.CRITICAL) {
            return SafetyDecision.Block("Motor hardware fault reported; motion prohibited.")
        }

        if (
            context.health.batteryHealthPercent < MIN_BATTERY_RESERVE_PERCENT &&
            command is RoboCommand.MoveToAngle
        ) {
            return SafetyDecision.Block("Battery below safe threshold (<10%). Conserving power.")
        }

        if (command is RoboCommand.MoveToAngle) {
            if (!command.angleDeg.isFinite()) {
                return SafetyDecision.Block("Requested angle must be finite.")
            }
            if (command.angleDeg > MAX_SAFE_ANGLE_DEG || command.angleDeg < MIN_SAFE_ANGLE_DEG) {
                val clamped = command.angleDeg.coerceIn(MIN_SAFE_ANGLE_DEG, MAX_SAFE_ANGLE_DEG)
                return SafetyDecision.Modify(
                    RoboCommand.MoveToAngle(clamped),
                    "Angle clamped to mechanical limit."
                )
            }
        }

        return SafetyDecision.Allow
    }
}
