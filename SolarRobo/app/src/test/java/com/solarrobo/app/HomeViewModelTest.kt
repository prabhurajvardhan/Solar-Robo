package com.solarrobo.app

import com.solarrobo.app.presentation.HomeViewModel
import com.solarrobo.app.di.TrackerRuntimeHolder
import com.solarrobo.core.ai.FakeAiEngine
import com.solarrobo.core.contracts.TrackerDailySummary
import com.solarrobo.core.contracts.TrackerLogRecord
import com.solarrobo.core.contracts.TrackerLogSink
import com.solarrobo.core.device.FakeRoboDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val device = FakeRoboDevice(initialConnected = false)
    private val aiEngine = FakeAiEngine()
    private lateinit var viewModel: HomeViewModel

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel(device, aiEngine, TrackerRuntimeHolder(), EmptyTrackerLogSink())
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun unconnectedDeviceHasNoInventedTelemetry() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }
        assertFalse(state.isConnected)
        assertNull(state.panelAngleDeg)
        assertNull(state.generationWatts)
        assertNull(state.batteryPercent)
        assertTrue(state.connectionText.contains("Offline"))
    }

    private class EmptyTrackerLogSink : TrackerLogSink {
        override suspend fun append(record: TrackerLogRecord) = Unit
        override suspend fun rollupAndPruneBefore(timestamp: Long) = Unit
        override fun observeRecent(limit: Int): Flow<List<TrackerLogRecord>> = flowOf(emptyList())
        override fun observeDailySummaries(): Flow<List<TrackerDailySummary>> = flowOf(emptyList())
    }
}
