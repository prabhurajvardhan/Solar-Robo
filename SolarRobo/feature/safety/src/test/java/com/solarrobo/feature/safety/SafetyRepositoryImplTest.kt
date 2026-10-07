package com.solarrobo.feature.safety

import com.solarrobo.core.contracts.CommandResult
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyDecision
import com.solarrobo.core.device.RoboDevice
import com.solarrobo.feature.safety.data.SafetyRepositoryImpl
import com.solarrobo.feature.safety.domain.SafetyPolicy
import com.solarrobo.feature.safety.mock.FakeSafetyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyRepositoryImplTest {
    @Test
    fun allowedCommandIsDispatchedAndBlockedCommandIsNot() = runTest {
        val device = RecordingRoboDevice()
        val repository = SafetyRepositoryImpl(device, SafetyPolicy())
        val healthy = FakeSafetyRepository.normalSafetyContext()
        val lowBattery = healthy.copy(
            health = healthy.health.copy(batteryHealthPercent = 1f)
        )

        assertEquals(
            SafetyDecision.Allow,
            repository.evaluateAndExecute(RoboCommand.MoveToAngle(45f), healthy)
        )
        assertTrue(
            repository.evaluateAndExecute(RoboCommand.MoveToAngle(45f), lowBattery) is SafetyDecision.Block
        )
        assertEquals(listOf(RoboCommand.MoveToAngle(45f)), device.commands)
        assertEquals("COMMAND_BLOCKED", repository.observeSafetyEvents().first().single().code)
    }

    @Test
    fun modifiedDecisionDispatchesModifiedCommand() = runTest {
        val device = RecordingRoboDevice()
        val repository = SafetyRepositoryImpl(device, SafetyPolicy())
        val context = FakeSafetyRepository.normalSafetyContext().copy(
            environment = FakeSafetyRepository.normalSafetyContext().environment.copy(windSpeedMps = 16f)
        )

        val decision = repository.evaluateAndExecute(RoboCommand.MoveToAngle(45f), context)

        assertTrue(decision is SafetyDecision.Modify)
        assertEquals(listOf(RoboCommand.SafePosition), device.commands)
    }

    @Test
    fun emergencyStopAndSafePositionDispatchCommandsAndRecordAcknowledgableEvents() = runTest {
        val device = RecordingRoboDevice()
        val repository = SafetyRepositoryImpl(device, SafetyPolicy())

        val stopResult = repository.emergencyStop("test stop")
        val stowResult = repository.safePosition("test stow")
        val events = repository.observeSafetyEvents().first()

        assertTrue(stopResult.accepted)
        assertTrue(stowResult.accepted)
        assertEquals(listOf(RoboCommand.StopMotion, RoboCommand.SafePosition), device.commands)
        assertEquals(listOf("E_STOP", "SAFE_POSITION"), events.map { it.code })
        assertEquals(listOf("test stop", "test stow"), events.map { it.message })
        assertFalse(events.any { it.acknowledged })

        repository.acknowledge(events.first().id)
        val acknowledged = repository.observeSafetyEvents().first()
        assertTrue(acknowledged.first().acknowledged)
        assertFalse(acknowledged.last().acknowledged)
    }

    @Test
    fun rejectedDeviceCommandIsRecordedAsIncident() = runTest {
        val device = RecordingRoboDevice(acceptCommands = false)
        val repository = SafetyRepositoryImpl(device, SafetyPolicy())

        repository.evaluateAndExecute(
            RoboCommand.MoveToAngle(45f),
            FakeSafetyRepository.normalSafetyContext()
        )

        assertEquals(
            listOf("COMMAND_REJECTED"),
            repository.observeSafetyEvents().first().map { it.code }
        )
    }

    @Test
    fun deviceFailureIsRecordedAndPropagated() = runTest {
        val device = RecordingRoboDevice(failCommands = true)
        val repository = SafetyRepositoryImpl(device, SafetyPolicy())

        try {
            repository.evaluateAndExecute(
                RoboCommand.MoveToAngle(45f),
                FakeSafetyRepository.normalSafetyContext()
            )
            throw AssertionError("Expected device failure to propagate.")
        } catch (exception: IllegalStateException) {
            assertEquals("device unavailable", exception.message)
        }

        assertEquals(
            listOf("COMMAND_DISPATCH_FAILED"),
            repository.observeSafetyEvents().first().map { it.code }
        )
    }

    private class RecordingRoboDevice(
        private val acceptCommands: Boolean = true,
        private val failCommands: Boolean = false
    ) : RoboDevice {
        val commands = mutableListOf<RoboCommand>()

        override suspend fun connect(): Result<Unit> = Result.success(Unit)
        override suspend fun disconnect() = Unit
        override suspend fun getSnapshot(): RoboSnapshot = error("Snapshot is not used by these tests.")

        override suspend fun sendCommand(command: RoboCommand): CommandResult {
            if (failCommands) error("device unavailable")
            commands += command
            return CommandResult(
                accepted = acceptCommands,
                commandId = "test-command-${commands.size}",
                reason = if (acceptCommands) null else "test rejection",
                timestamp = commands.size.toLong()
            )
        }

        override fun observeSnapshot(): Flow<RoboSnapshot> = flowOf()
    }
}
