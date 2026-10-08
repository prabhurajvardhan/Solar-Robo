package com.solarrobo.feature.energy

import com.solarrobo.feature.energy.domain.CalculateEnergyMetrics
import com.solarrobo.feature.energy.domain.EnergyStatus
import com.solarrobo.feature.energy.mock.EnergyScenario
import com.solarrobo.feature.energy.mock.FakeEnergyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyRepositoryTest {

    private val calculateMetrics = CalculateEnergyMetrics()

    @Test
    fun fakeRepository_returnsDeterministicNormalSnapshot() = runTest {
        val repository = FakeEnergyRepository(EnergyScenario.NORMAL)
        val snapshot = repository.getEnergySnapshot().first()
        val history = repository.getEnergyHistory().first()
        val error = repository.getErrorMessage().first()

        assertEquals(420.0f, snapshot.generatedWatts, 0.001f)
        assertEquals(85.0f, snapshot.consumedWatts, 0.001f)
        assertEquals(78.0f, snapshot.batteryPercent, 0.001f)
        assertEquals(4, history.size)
        assertNull(error)
    }

    @Test
    fun calculateMetrics_normalScenarioYieldsHighGenerationAndPositiveNet() {
        val snapshot = FakeEnergyRepository.createSnapshotForScenario(EnergyScenario.NORMAL)
        val history = FakeEnergyRepository.createHistoryForScenario(EnergyScenario.NORMAL)

        val metrics = calculateMetrics(snapshot, history)

        assertEquals(EnergyStatus.HIGH_GENERATION, metrics.status)
        assertEquals(335.0f, metrics.netPowerWatts, 0.001f)
        assertTrue(metrics.isCharging)
        assertTrue(metrics.totalHistoricalYieldWh > 0f)
        assertTrue(metrics.averageSolarEfficiency > 0.9f)
    }

    @Test
    fun calculateMetrics_lowBatteryTriggersConservation() {
        val snapshot = FakeEnergyRepository.createSnapshotForScenario(EnergyScenario.LOW_BATTERY)
        val history = FakeEnergyRepository.createHistoryForScenario(EnergyScenario.LOW_BATTERY)

        val metrics = calculateMetrics(snapshot, history)

        assertEquals(EnergyStatus.ENERGY_CONSERVATION, metrics.status)
    }

    @Test
    fun calculateMetrics_lowGenerationSetsDischargingOrLowGen() {
        val snapshot = FakeEnergyRepository.createSnapshotForScenario(EnergyScenario.LOW_GENERATION)
        val history = FakeEnergyRepository.createHistoryForScenario(EnergyScenario.LOW_GENERATION)

        val metrics = calculateMetrics(snapshot, history)

        assertEquals(EnergyStatus.LOW_GENERATION, metrics.status)
        assertEquals(-40.0f, metrics.netPowerWatts, 0.001f)
        assertFalse(metrics.isCharging)
    }

    @Test
    fun repository_emptyHistoryScenarioReturnsZeroHistoricalYield() = runTest {
        val repository = FakeEnergyRepository(EnergyScenario.EMPTY_HISTORY)
        val history = repository.getEnergyHistory().first()
        val snapshot = repository.getEnergySnapshot().first()

        assertTrue(history.isEmpty())
        val metrics = calculateMetrics(snapshot, history)
        assertEquals(0.0f, metrics.totalHistoricalYieldWh, 0.001f)
        assertEquals(0.0f, metrics.averageSolarEfficiency, 0.001f)
    }
}
