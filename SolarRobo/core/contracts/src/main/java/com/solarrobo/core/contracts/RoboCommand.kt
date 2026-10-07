package com.solarrobo.core.contracts

sealed interface RoboCommand {
    data class MoveToAngle(val angleDeg: Float) : RoboCommand
    data object StopMotion : RoboCommand
    data object SafePosition : RoboCommand
}

data class CommandResult(
    val accepted: Boolean,
    val commandId: String,
    val reason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
