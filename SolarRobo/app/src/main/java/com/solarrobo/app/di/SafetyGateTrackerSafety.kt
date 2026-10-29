package com.solarrobo.app.di

import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.SafetyContext
import com.solarrobo.core.contracts.SafetyDecision
import com.solarrobo.core.contracts.SafetyGate
import com.solarrobo.core.tracker.TrackerSafety

class SafetyGateTrackerSafety(private val gate: SafetyGate) : TrackerSafety {
    override suspend fun authorize(operation: String, context: SafetyContext): Boolean {
        val command = when {
            operation.startsWith("safe-position") -> RoboCommand.SafePosition
            operation == "stop" -> RoboCommand.StopMotion
            operation == "home" || operation == "set-limits" || operation == "sync" || operation.contains("rehome") -> RoboCommand.Home
            else -> RoboCommand.MoveToAngle(0f)
        }
        return when (val decision = gate.evaluate(command, context)) {
            SafetyDecision.Allow -> true
            is SafetyDecision.Modify -> decision.command == command
            is SafetyDecision.Block -> false
        }
    }
}
