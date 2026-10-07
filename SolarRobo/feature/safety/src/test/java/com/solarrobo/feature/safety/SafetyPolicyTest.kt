package com.solarrobo.feature.safety

import com.solarrobo.core.contracts.HealthStatus
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.SafetyDecision
import com.solarrobo.feature.safety.domain.SafetyPolicy
import com.solarrobo.feature.safety.mock.FakeSafetyRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyPolicyTest {
    private val policy = SafetyPolicy()
    private val healthy = FakeSafetyRepository.normalSafetyContext()

    @Test
    fun healthyCommandIsAllowed() {
        assertEquals(SafetyDecision.Allow, policy.evaluate(RoboCommand.MoveToAngle(45f), healthy))
    }

    @Test
    fun documentedAngleBoundariesAndInteriorValuesAreAllowed() {
        listOf(-85f, -84.999f, 0f, 84.999f, 85f).forEach { angle ->
            assertEquals(SafetyDecision.Allow, policy.evaluate(RoboCommand.MoveToAngle(angle), healthy))
        }
    }

    @Test
    fun anglesOutsideDocumentedBoundsAreModified() {
        assertEquals(
            SafetyDecision.Modify(RoboCommand.MoveToAngle(-85f), "Angle clamped to mechanical limit."),
            policy.evaluate(RoboCommand.MoveToAngle(-85.001f), healthy)
        )
        assertEquals(
            SafetyDecision.Modify(RoboCommand.MoveToAngle(85f), "Angle clamped to mechanical limit."),
            policy.evaluate(RoboCommand.MoveToAngle(85.001f), healthy)
        )
    }

    @Test
    fun windAboveDocumentedThresholdModifiesToSafePosition() {
        val atThreshold = healthy.copy(
            environment = healthy.environment.copy(
                windSpeedMps = SafetyPolicy.MAX_SAFE_WIND_SPEED_MPS
            )
        )
        assertEquals(SafetyDecision.Allow, policy.evaluate(RoboCommand.MoveToAngle(45f), atThreshold))

        val windy = healthy.copy(
            environment = healthy.environment.copy(
                windSpeedMps = SafetyPolicy.MAX_SAFE_WIND_SPEED_MPS + 0.1f
            )
        )
        val decision = policy.evaluate(RoboCommand.MoveToAngle(45f), windy)

        assertTrue(decision is SafetyDecision.Modify)
        assertEquals(RoboCommand.SafePosition, (decision as SafetyDecision.Modify).command)
    }

    @Test
    fun lowBatteryBlocksAngleMovementAndExactThresholdIsAllowed() {
        val atThreshold = healthy.copy(
            health = healthy.health.copy(
                batteryHealthPercent = SafetyPolicy.MIN_BATTERY_RESERVE_PERCENT
            )
        )
        val belowThreshold = atThreshold.copy(
            health = atThreshold.health.copy(
                batteryHealthPercent = SafetyPolicy.MIN_BATTERY_RESERVE_PERCENT - 0.1f
            )
        )

        assertEquals(SafetyDecision.Allow, policy.evaluate(RoboCommand.MoveToAngle(45f), atThreshold))
        assertTrue(policy.evaluate(RoboCommand.MoveToAngle(45f), belowThreshold) is SafetyDecision.Block)
        assertEquals(SafetyDecision.Allow, policy.evaluate(RoboCommand.StopMotion, belowThreshold))
    }

    @Test
    fun canonicalCriticalMotorStatusBlocksMovement() {
        val criticalMotor = healthy.copy(
            health = healthy.health.copy(motorStatus = HealthStatus.CRITICAL)
        )
        assertTrue(policy.evaluate(RoboCommand.MoveToAngle(45f), criticalMotor) is SafetyDecision.Block)
    }

    @Test
    fun nonFiniteAngleIsBlocked() {
        assertTrue(policy.evaluate(RoboCommand.MoveToAngle(Float.NaN), healthy) is SafetyDecision.Block)
    }

    @Test
    fun repeatedEvaluationIsDeterministic() {
        val command = RoboCommand.MoveToAngle(100f)
        assertEquals(
            policy.evaluate(command, healthy),
            policy.evaluate(command, healthy.copy())
        )
    }
}
