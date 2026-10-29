package com.solarrobo.core.contracts

/**
 * Commands dispatched to adjust tracker position or invoke safety stow.
 */
sealed interface RoboCommand {
    /** Request solar tracker to rotate to a specific azimuth/elevation angle */
    data class MoveToAngle(val angleDeg: Float) : RoboCommand

    /** Immediately stop motor actuation */
    data object StopMotion : RoboCommand

    /** Declare that the panel is physically flat now; this does not itself move the motor. */
    data object Home : RoboCommand

    /** Request a calibrated controller to move the panel to flat (0 deg). */
    data object SafePosition : RoboCommand
}

/**
 * Result emitted after a command is evaluated and dispatched.
 */
data class CommandResult(
    val accepted: Boolean,
    val commandId: String,
    val reason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
