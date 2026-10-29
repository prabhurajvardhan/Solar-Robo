package com.solarrobo.core.tracker

import com.solarrobo.core.contracts.LinkState
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackerControllerTest {
    @Test fun offlineLinkPreventsStartupActuation() = runBlocking {
        val motor = RecordingMotor(connected = false)
        val controller = TrackerController(
            motor = motor,
            frames = FrameSource { null },
            clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC),
            tuning = ControllerTuning(37.7, -122.4, cycle = Duration.ofSeconds(1)),
            store = EmptyStore,
            safety = TrackerSafety { _, _ -> true }
        )

        controller.start()

        assertEquals(LinkState.OFFLINE, controller.observeState().value.link)
        assertEquals(0, motor.limitCalls)
        assertEquals(0, motor.moveCalls)
    }

    private object EmptyStore : TrackerStateStore {
        override suspend fun load(): PersistedTrackerState? = null
        override suspend fun save(state: PersistedTrackerState) = Unit
    }

    private class RecordingMotor(private val connected: Boolean) : Motor {
        var limitCalls = 0
        var moveCalls = 0
        override suspend fun move(direction: MoveDirection, milliseconds: Long): Long { moveCalls++; return milliseconds }
        override suspend fun status() = MotorStatus(connected = connected)
        override suspend fun stop() = Unit
        override suspend fun home() = true
        override suspend fun sync(thetaDeg: Double) = Unit
        override suspend fun setLimit(minThetaDeg: Double, maxThetaDeg: Double) { limitCalls++ }
    }
}
