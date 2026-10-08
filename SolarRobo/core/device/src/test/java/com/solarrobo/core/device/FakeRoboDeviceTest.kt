package com.solarrobo.core.device

import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.RoboMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeRoboDeviceTest {

    private val device = FakeRoboDevice()

    @Test
    fun testConnectAndDisconnect() = runTest {
        device.connect()
        assertTrue(device.getSnapshot().connected)

        device.disconnect()
        assertFalse(device.getSnapshot().connected)
    }

    @Test
    fun testSendCommandMoveToAngle() = runTest {
        val result = device.sendCommand(RoboCommand.MoveToAngle(30.0f))
        assertTrue(result.accepted)

        val snapshot = device.observeSnapshot().first()
        assertEquals(30.0f, snapshot.panelAngleDeg, 0.001f)
    }

    @Test
    fun testSendCommandSafePosition() = runTest {
        val result = device.sendCommand(RoboCommand.SafePosition)
        assertTrue(result.accepted)

        val snapshot = device.getSnapshot()
        assertEquals(0.0f, snapshot.panelAngleDeg, 0.001f)
        assertEquals(RoboMode.SAFE, snapshot.mode)
    }
}
