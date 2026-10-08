package com.solarrobo.app

import com.solarrobo.app.presentation.HomeViewModel
import com.solarrobo.core.ai.FakeAiEngine
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.core.simulator.SimulationScenario
import com.solarrobo.core.simulator.SimulatorEngineImpl
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val simulatorEngine = SimulatorEngineImpl()
    private val aiEngine = FakeAiEngine()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel(simulatorEngine, aiEngine)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUiStateReflectsSimulatorAndAiState() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.first { !it.isLoading }

        assertTrue(state.isConnected)
        assertEquals(RoboMode.NORMAL, state.mode)
        assertEquals(45f, state.panelAngleDeg, 0.001f)
        assertEquals(320f, state.generationWatts, 0.001f)
        assertEquals(94f, state.batteryPercent, 0.001f)
        assertEquals(SafetyLevel.NORMAL, state.safetyLevel)
        assertTrue(state.latestMessage.contains("operating normally"))
    }
}
