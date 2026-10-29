package com.solarrobo.core.contracts

import java.util.concurrent.atomic.AtomicBoolean

/** Facts required before any motion decision; `panelAngleDeg` remains an estimate, not encoder data. */
data class SafetyContext(
    val link: LinkState,
    val statusFresh: Boolean,
    val motorBusy: Boolean,
    val calibrated: Boolean,
    val faultActive: Boolean,
    val windHold: Boolean,
    val calibrationInProgress: Boolean = false
)

/** Stateless policy plus a thread-safe E-stop latch. Callers must evaluate before every motion. */
interface SafetyLatchStore {
    fun isLatched(): Boolean
    fun setLatched(latched: Boolean)
}

class SafetyGate(
    private val latchStore: SafetyLatchStore = object : SafetyLatchStore {
        override fun isLatched() = false
        override fun setLatched(latched: Boolean) = Unit
    }
) {
    private val loadResult = runCatching { latchStore.isLatched() }
    private val emergencyLatched = AtomicBoolean(loadResult.getOrDefault(true))
    @Volatile private var latchPersisted = loadResult.isSuccess

    fun triggerEmergencyStop() {
        emergencyLatched.set(true)
        latchPersisted = runCatching { latchStore.setLatched(true) }.isSuccess
    }
    fun resumeAfterEmergencyStop() {
        emergencyLatched.set(false)
        latchPersisted = runCatching { latchStore.setLatched(false) }.isSuccess
    }
    fun isEmergencyLatched(): Boolean = emergencyLatched.get()
    fun isLatchPersisted(): Boolean = latchPersisted

    fun evaluate(command: RoboCommand, context: SafetyContext): SafetyDecision {
        if (command == RoboCommand.StopMotion) return SafetyDecision.Allow
        if (emergencyLatched.get()) return SafetyDecision.Block("Emergency stop is latched; explicitly resume first")
        if (context.link != LinkState.CONNECTED) return SafetyDecision.Block("Tracker link is not connected")
        if (!context.statusFresh) return SafetyDecision.Block("ESP32 status is stale")
        if (context.faultActive) return SafetyDecision.Block("Active fault blocks all motion until reset")
        if (context.motorBusy) return SafetyDecision.Block("Motor is already running")

        return when (command) {
            RoboCommand.StopMotion -> SafetyDecision.Allow
            RoboCommand.Home -> SafetyDecision.Allow
            RoboCommand.SafePosition -> {
                if (!context.calibrated) SafetyDecision.Block("Calibration is required before stowing")
                else SafetyDecision.Allow
            }
            is RoboCommand.MoveToAngle -> when {
                !context.calibrated && !context.calibrationInProgress -> SafetyDecision.Block("Calibration is required before tracking")
                context.windHold -> SafetyDecision.Block("Wind-risk heuristic hold blocks tracking motion")
                command.angleDeg !in -70f..70f -> SafetyDecision.Modify(
                    RoboCommand.MoveToAngle(command.angleDeg.coerceIn(-70f, 70f)),
                    "Requested target was clamped to the +/-70 degree travel limit"
                )
                else -> SafetyDecision.Allow
            }
        }
    }
}
