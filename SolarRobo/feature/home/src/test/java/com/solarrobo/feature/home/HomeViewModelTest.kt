package com.solarrobo.feature.home

import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.home.mock.FakeHomeRepository
import com.solarrobo.feature.home.mock.HomeScenario
import com.solarrobo.feature.home.presentation.HomeUiState
import com.solarrobo.feature.home.presentation.HomeViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
    fun initialState_loadsDataDeterministically() = runTest(dispatcher) {
        val repository = FakeHomeRepository(HomeScenario.NORMAL)
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.first { it is HomeUiState.Success }
        assertTrue(state is HomeUiState.Success)
        val success = state as HomeUiState.Success
        assertEquals(45.0f, success.robo.panelAngleDeg, 0.001f)
        assertEquals(320.0f, success.energy.generatedWatts, 0.001f)
        assertEquals(SafetyLevel.NORMAL, success.safetyLevel)
        assertEquals(RoboMode.NORMAL, success.robo.mode)
        assertTrue(success.robo.connected)
        assertTrue(success.activeAlerts.isEmpty())
    }

    @Test
    fun errorState_emitsErrorMessage() = runTest(dispatcher) {
        val repository = FakeHomeRepository(HomeScenario.ERROR)
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.first { it is HomeUiState.Error }
        assertTrue(state is HomeUiState.Error)
        assertEquals("Command center telemetry unavailable.", (state as HomeUiState.Error).message)
    }

    @Test
    fun protectingState_reflectsSafetyEvents() = runTest(dispatcher) {
        val repository = FakeHomeRepository(HomeScenario.PROTECTING)
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.first { it is HomeUiState.Success }
        val success = state as HomeUiState.Success
        assertEquals(SafetyLevel.PROTECTING, success.safetyLevel)
        assertEquals(RoboMode.PROTECTING, success.robo.mode)
        assertFalse(success.activeAlerts.isEmpty())
        assertEquals("WIND_STOW", success.activeAlerts.first().code)
    }

    @Test
    fun refresh_updatesState() = runTest(dispatcher) {
        val repository = FakeHomeRepository(HomeScenario.NORMAL)
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.first { it is HomeUiState.Success }
        assertTrue(state is HomeUiState.Success)
    }
}
