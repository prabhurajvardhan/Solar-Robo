package com.solarrobo.feature.simulator

import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.simulator.data.SimulatorRepositoryImpl
import com.solarrobo.feature.simulator.domain.Scenario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatorViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultState_isDeterministic() = runTest(dispatcher) {
        val first = SimulatorRepositoryImpl()
        val second = SimulatorRepositoryImpl()

        assertEquals(first.getRoboSnapshot().first(), second.getRoboSnapshot().first())
        assertEquals(first.getEnergySnapshot().first(), second.getEnergySnapshot().first())
        assertEquals(first.getEnvironmentSnapshot().first(), second.getEnvironmentSnapshot().first())
        assertEquals(0L, first.getRoboSnapshot().first().timestamp)
    }

    @Test
    fun sunnyScenario_resetsToFixedNormalValues() = runTest(dispatcher) {
        val repository = SimulatorRepositoryImpl()
        repository.applyScenario(Scenario.SUNNY_NORMAL)

        val robo = repository.getRoboSnapshot().first()
        val energy = repository.getEnergySnapshot().first()
        val environment = repository.getEnvironmentSnapshot().first()

        assertTrue(robo.connected)
        assertEquals(RoboMode.NORMAL, robo.mode)
        assertEquals(320f, energy.generatedWatts)
        assertEquals(92f, energy.batteryPercent)
        assertEquals(85_000f, environment.lightLux)
        assertFalse(environment.rainDetected)
    }

    @Test
    fun lowLight_hasLowerDeterministicGeneration() = runTest(dispatcher) {
        val repository = SimulatorRepositoryImpl()
        repository.applyScenario(Scenario.LOW_LIGHT)

        assertEquals(5_000f, repository.getEnvironmentSnapshot().first().lightLux)
        assertEquals(40f, repository.getEnergySnapshot().first().generatedWatts)
        assertTrue(repository.getEnergySnapshot().first().generatedWatts < 320f)
    }

    @Test
    fun highWind_reportsConditionsWithoutApplyingSafetyDecision() = runTest(dispatcher) {
        val repository = SimulatorRepositoryImpl()
        repository.applyScenario(Scenario.HIGH_WIND)

        assertEquals(18.2f, repository.getEnvironmentSnapshot().first().windSpeedMps)
        assertEquals(RoboMode.NORMAL, repository.getRoboSnapshot().first().mode)
    }

    @Test
    fun rainScenario_reportsRainAndReducedLight() = runTest(dispatcher) {
        val repository = SimulatorRepositoryImpl()
        repository.applyScenario(Scenario.RAIN)

        assertTrue(repository.getEnvironmentSnapshot().first().rainDetected)
        assertEquals(15_000f, repository.getEnvironmentSnapshot().first().lightLux)
        assertEquals(60f, repository.getEnergySnapshot().first().generatedWatts)
    }

    @Test
    fun motorJam_marksHealthFault_andDoesNotTrack() = runTest(dispatcher) {
        val repository = SimulatorRepositoryImpl()
        repository.applyScenario(Scenario.MOTOR_JAM)
        val initialAngle = repository.getRoboSnapshot().first().panelAngleDeg
        repository.tick(1_000L)

        assertEquals(RoboMode.FAULT, repository.getRoboSnapshot().first().mode)
        assertEquals(initialAngle, repository.getRoboSnapshot().first().panelAngleDeg)
        assertEquals(com.solarrobo.core.contracts.HealthStatus.CRITICAL, repository.getDeviceHealth().first().motorStatus)
        assertEquals("MOTOR_STALL_01", repository.getSafetyEvents().first().single().code)
    }

    @Test
    fun batteryLow_reportsFixedChargeAndConservingMode() = runTest(dispatcher) {
        val repository = SimulatorRepositoryImpl()
        repository.applyScenario(Scenario.BATTERY_LOW)

        assertEquals(8f, repository.getEnergySnapshot().first().batteryPercent)
        assertEquals(RoboMode.CONSERVING, repository.getRoboSnapshot().first().mode)
    }

    @Test
    fun deviceOffline_isRepresentedWithoutSuccessfulDeviceCommands() = runTest(dispatcher) {
        val repository = SimulatorRepositoryImpl()
        repository.applyScenario(Scenario.DEVICE_OFFLINE)
        repository.tick(1_000L)

        assertFalse(repository.getRoboSnapshot().first().connected)
        assertEquals(com.solarrobo.core.contracts.GridState.UNKNOWN, repository.getEnergySnapshot().first().gridState)
        assertEquals(com.solarrobo.core.contracts.HealthStatus.OFFLINE, repository.getDeviceHealth().first().overallStatus)
        assertEquals(1_000L, repository.getRoboSnapshot().first().timestamp)
    }

    @Test
    fun tick_advancesTimeAndTrackerDeterministically() = runTest(dispatcher) {
        val repository = SimulatorRepositoryImpl()
        repository.applyScenario(Scenario.SUNNY_NORMAL)
        repository.tick(1_000L)
        repository.tick(1_000L)

        val robo = repository.getRoboSnapshot().first()
        assertEquals(2_000L, robo.timestamp)
        assertEquals(35.5f, robo.panelAngleDeg, 0.0001f)
        assertTrue(repository.getEnergySnapshot().first().batteryPercent >= 92f)
    }

    @Test
    fun repeatedScenarioAndTicks_produceEqualSnapshots() = runTest(dispatcher) {
        val first = SimulatorRepositoryImpl()
        val second = SimulatorRepositoryImpl()
        listOf(first, second).forEach { repository ->
            repository.applyScenario(Scenario.SUNNY_NORMAL)
            repository.tick(1_000L)
            repository.tick(1_000L)
        }

        assertEquals(first.getRoboSnapshot().first(), second.getRoboSnapshot().first())
        assertEquals(first.getEnergySnapshot().first(), second.getEnergySnapshot().first())
        assertEquals(first.getEnvironmentSnapshot().first(), second.getEnvironmentSnapshot().first())
    }

    @Test
    fun switchingScenarios_replacesPreviousEnvironmentValues() = runTest(dispatcher) {
        val repository = SimulatorRepositoryImpl()
        repository.applyScenario(Scenario.HIGH_WIND)
        repository.applyScenario(Scenario.SUNNY_NORMAL)

        val environment = repository.getEnvironmentSnapshot().first()
        assertEquals(3.5f, environment.windSpeedMps)
        assertFalse(environment.rainDetected)
        assertEquals(85_000f, environment.lightLux)
    }

    @Test
    fun injectedFault_hasRepeatableEventIdentityAndTimestamp() = runTest(dispatcher) {
        val first = SimulatorRepositoryImpl()
        val second = SimulatorRepositoryImpl()

        val firstEvent = first.injectFault("MOTOR_STALL_01", "Simulated motor stall.")
        val secondEvent = second.injectFault("MOTOR_STALL_01", "Simulated motor stall.")

        assertEquals(firstEvent, secondEvent)
        assertEquals("SIM-EVENT-000001", firstEvent.id)
        assertEquals(SafetyLevel.FAULT, firstEvent.level)
    }

    @Test
    fun tick_rejectsNegativeElapsedTime() = runTest(dispatcher) {
        val repository = SimulatorRepositoryImpl()

        try {
            repository.tick(-1L)
            throw AssertionError("Expected negative simulated time to be rejected")
        } catch (error: IllegalArgumentException) {
            assertEquals("Simulated elapsed time must not be negative.", error.message)
        }
    }
}