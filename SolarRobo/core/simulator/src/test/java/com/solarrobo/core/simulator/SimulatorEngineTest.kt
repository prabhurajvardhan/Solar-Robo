package com.solarrobo.core.simulator

import com.solarrobo.core.contracts.HealthStatus
import com.solarrobo.core.contracts.RoboMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatorEngineTest {

    private val engine = SimulatorEngineImpl()

    @Test
    fun testNormalScenarioProducesDeterministicState() = runTest {
        engine.setScenario(SimulationScenario.NORMAL)

        val robo = engine.observeRoboSnapshot().first()
        val energy = engine.observeEnergySnapshot().first()
        val env = engine.observeEnvironmentSnapshot().first()
        val health = engine.observeDeviceHealth().first()

        assertTrue(robo.connected)
        assertEquals(RoboMode.NORMAL, robo.mode)
        assertEquals(45f, robo.panelAngleDeg, 0.001f)
        assertEquals(320f, energy.generatedWatts, 0.001f)
        assertEquals(94f, energy.batteryPercent, 0.001f)
        assertFalse(env.rainDetected)
        assertEquals(HealthStatus.HEALTHY, health.overallStatus)
    }

    @Test
    fun testHighWindScenarioForcesStow() = runTest {
        engine.setScenario(SimulationScenario.HIGH_WIND)

        val robo = engine.observeRoboSnapshot().first()
        val env = engine.observeEnvironmentSnapshot().first()
        val events = engine.observeSafetyEvents().first()

        assertEquals(RoboMode.SAFE, robo.mode)
        assertEquals(0f, robo.panelAngleDeg, 0.001f)
        assertTrue(env.windSpeedMps > 15f)
        assertTrue(events.isNotEmpty())
        assertEquals("SAFE_WIND_01", events.first().code)
    }

    @Test
    fun testOfflineScenarioReportsDisconnected() = runTest {
        engine.setScenario(SimulationScenario.DEVICE_OFFLINE)

        val robo = engine.observeRoboSnapshot().first()
        val health = engine.observeDeviceHealth().first()

        assertFalse(robo.connected)
        assertEquals(HealthStatus.OFFLINE, health.overallStatus)
    }
}
