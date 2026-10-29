package com.solarrobo.core.contracts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyGateTest {
    private val ready = SafetyContext(LinkState.CONNECTED, true, false, true, false, false)

    @Test fun clampsTravelLimit() {
        val decision = SafetyGate().evaluate(RoboCommand.MoveToAngle(80f), ready)
        assertTrue(decision is SafetyDecision.Modify)
        assertEquals(RoboCommand.MoveToAngle(70f), (decision as SafetyDecision.Modify).command)
    }

    @Test fun blocksUncalibratedFaultedBusyAndOfflineMotion() {
        val gate = SafetyGate()
        assertTrue(gate.evaluate(RoboCommand.MoveToAngle(5f), ready.copy(calibrated = false)) is SafetyDecision.Block)
        assertTrue(gate.evaluate(RoboCommand.MoveToAngle(5f), ready.copy(faultActive = true)) is SafetyDecision.Block)
        assertTrue(gate.evaluate(RoboCommand.MoveToAngle(5f), ready.copy(motorBusy = true)) is SafetyDecision.Block)
        assertTrue(gate.evaluate(RoboCommand.MoveToAngle(5f), ready.copy(link = LinkState.OFFLINE)) is SafetyDecision.Block)
        assertTrue(gate.evaluate(RoboCommand.MoveToAngle(5f), ready.copy(statusFresh = false)) is SafetyDecision.Block)
    }

    @Test fun windHoldBlocksTrackingButStopRemainsAvailable() {
        val gate = SafetyGate()
        assertTrue(gate.evaluate(RoboCommand.MoveToAngle(5f), ready.copy(windHold = true)) is SafetyDecision.Block)
        assertEquals(SafetyDecision.Allow, gate.evaluate(RoboCommand.StopMotion, ready.copy(link = LinkState.OFFLINE)))
    }

    @Test fun emergencyStopRequiresExplicitResume() {
        val gate = SafetyGate()
        gate.triggerEmergencyStop()
        assertTrue(gate.evaluate(RoboCommand.SafePosition, ready) is SafetyDecision.Block)
        gate.resumeAfterEmergencyStop()
        assertEquals(SafetyDecision.Allow, gate.evaluate(RoboCommand.SafePosition, ready))
    }

    @Test fun emergencyLatchSurvivesGateRecreation() {
        var persisted = false
        val store = object : SafetyLatchStore {
            override fun isLatched() = persisted
            override fun setLatched(latched: Boolean) { persisted = latched }
        }
        SafetyGate(store).triggerEmergencyStop()
        val restored = SafetyGate(store)
        assertTrue(restored.isEmergencyLatched())
        assertTrue(restored.evaluate(RoboCommand.SafePosition, ready) is SafetyDecision.Block)
        restored.resumeAfterEmergencyStop()
        assertTrue(!SafetyGate(store).isEmergencyLatched())
    }

    @Test fun calibrationBypassesOnlyTheCalibrationRequiredInterlock() {
        val gate = SafetyGate()
        val calibration = ready.copy(calibrated = false, calibrationInProgress = true)
        assertEquals(SafetyDecision.Allow, gate.evaluate(RoboCommand.MoveToAngle(0f), calibration))
        assertTrue(gate.evaluate(RoboCommand.MoveToAngle(0f), calibration.copy(windHold = true)) is SafetyDecision.Block)
        assertTrue(gate.evaluate(RoboCommand.MoveToAngle(0f), calibration.copy(faultActive = true)) is SafetyDecision.Block)
        gate.triggerEmergencyStop()
        assertTrue(gate.evaluate(RoboCommand.MoveToAngle(0f), calibration) is SafetyDecision.Block)
    }
}
