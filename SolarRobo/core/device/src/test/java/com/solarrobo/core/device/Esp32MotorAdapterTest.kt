package com.solarrobo.core.device

import com.solarrobo.core.tracker.MotorLimitReachedException
import com.solarrobo.core.tracker.MoveDirection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Esp32MotorAdapterTest {
    @Test
    fun partialClampWaitsForCompletionAndReturnsActualTravel() = runTest {
        val transport = FakeTransport(
            before = FirmwareStatus(netMs = 9_000, limitMs = 10_000, running = false, ranMs = 0, clamped = false),
            accepted = FirmwareStatus(netMs = 9_000, limitMs = 10_000, running = true, ranMs = 1_000, clamped = true),
            after = FirmwareStatus(netMs = 10_000, limitMs = 10_000, running = false, ranMs = 0, clamped = false)
        )
        val motor = Esp32MotorAdapter({ transport }, { 2.4 }, pollIntervalMs = 0, moveTimeoutMs = 1_000)

        val actualMs = motor.move(MoveDirection.FORWARD, 2_000)

        assertEquals(1_000L, actualMs)
        assertEquals(1, transport.moveCalls)
        assertEquals(2, transport.statusCalls)
    }

    @Test
    fun zeroDistanceClampRaisesLimitReached() = runTest {
        val status = FirmwareStatus(netMs = 10_000, limitMs = 10_000, running = false, ranMs = 0, clamped = true)
        val transport = FakeTransport(before = status, accepted = status, after = status)
        val motor = Esp32MotorAdapter({ transport }, { 2.4 }, pollIntervalMs = 0, moveTimeoutMs = 1_000)
        var limitReached = false

        try {
            motor.move(MoveDirection.FORWARD, 2_000)
        } catch (_: MotorLimitReachedException) {
            limitReached = true
        }

        assertTrue(limitReached)
        assertEquals(1, transport.moveCalls)
        assertEquals(1, transport.statusCalls)
    }

    private class FakeTransport(
        private val before: FirmwareStatus,
        private val accepted: FirmwareStatus,
        private val after: FirmwareStatus
    ) : Esp32Transport {
        var moveCalls = 0
        var statusCalls = 0
        private var moveStarted = false

        override suspend fun status(): FirmwareStatus {
            statusCalls++
            return if (moveStarted) after else before
        }

        override suspend fun move(direction: Int, durationMs: Int): FirmwareStatus {
            moveCalls++
            moveStarted = true
            return accepted
        }

        override suspend fun stop() = after
        override suspend fun home() = before
        override suspend fun sync(signedMs: Long) = before
        override suspend fun setLimit(limitMs: Long) = before
    }
}
