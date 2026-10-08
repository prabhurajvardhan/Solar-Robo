package com.solarrobo.feature.energy

import com.solarrobo.feature.energy.domain.CalculateEnergyMetrics
import com.solarrobo.feature.energy.domain.EnergyStatus
import com.solarrobo.feature.energy.mock.EnergyScenario
import com.solarrobo.feature.energy.mock.FakeEnergyRepository
import com.solarrobo.feature.energy.presentation.EnergyUiState
import com.solarrobo.feature.energy.presentation.EnergyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EnergyViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val calculateMetrics = CalculateEnergyMetrics()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun viewModel_loadsSuccessStateDeterministically() = runTest(dispatcher) {
        val repository = FakeEnergyRepository(EnergyScenario.NORMAL)
        val viewModel = EnergyViewModel(repository, calculateMetrics)

        advanceUntilIdle()

        val state = viewModel.uiState.first { it is EnergyUiState.Success }
        assertTrue(state is EnergyUiState.Success)
        val success = state as EnergyUiState.Success
        assertEquals(420.0f, success.snapshot.generatedWatts, 0.001f)
        assertEquals(78.0f, success.snapshot.batteryPercent, 0.001f)
        assertEquals(EnergyStatus.HIGH_GENERATION, success.metrics.status)
        assertEquals(4, success.history.size)
    }

    @Test
    fun viewModel_handlesErrorState() = runTest(dispatcher) {
        val repository = FakeEnergyRepository(EnergyScenario.ERROR)
        val viewModel = EnergyViewModel(repository, calculateMetrics)

        advanceUntilIdle()

        val state = viewModel.uiState.first { it is EnergyUiState.Error }
        assertTrue(state is EnergyUiState.Error)
        assertEquals("Energy telemetry sensor unavailable.", (state as EnergyUiState.Error).message)
    }

    @Test
    fun viewModel_handlesEmptyHistoryInSuccessState() = runTest(dispatcher) {
        val repository = FakeEnergyRepository(EnergyScenario.EMPTY_HISTORY)
        val viewModel = EnergyViewModel(repository, calculateMetrics)

        advanceUntilIdle()

        val state = viewModel.uiState.first { it is EnergyUiState.Success }
        val success = state as EnergyUiState.Success
        assertTrue(success.isHistoryEmpty)
        assertEquals(0, success.history.size)
    }
}
