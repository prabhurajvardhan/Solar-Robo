package com.solarrobo.feature.safety

import com.solarrobo.core.contracts.CommandResult
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.SafetyDecision
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.safety.domain.SafetyContext
import com.solarrobo.feature.safety.domain.SafetyRepository
import com.solarrobo.feature.safety.mock.FakeSafetyRepository
import com.solarrobo.feature.safety.presentation.SafetyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SafetyViewModelTest {
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
    fun observingAndEvaluatingExposeLoadingThenSuccess() = runTest(dispatcher) {
        val repository = FakeSafetyRepository()
        val viewModel = SafetyViewModel(repository)

        assertTrue(viewModel.uiState.value.isLoading)
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.evaluate(
            RoboCommand.MoveToAngle(45f),
            FakeSafetyRepository.normalSafetyContext()
        )
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(SafetyDecision.Allow, viewModel.uiState.value.lastDecision)
        assertEquals(SafetyLevel.NORMAL, viewModel.uiState.value.currentLevel)
    }

    @Test
    fun emergencyStopPublishesResultAndIncident() = runTest(dispatcher) {
        val viewModel = SafetyViewModel(FakeSafetyRepository())
        runCurrent()

        viewModel.emergencyStop("test stop")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.lastCommandResult?.accepted == true)
        assertTrue(viewModel.uiState.value.isEmergencyActive)
        assertEquals(SafetyLevel.EMERGENCY, viewModel.uiState.value.currentLevel)
        assertEquals(1, viewModel.uiState.value.activeIncidents.size)
    }

    @Test
    fun acknowledgementClearsActiveEmergencyState() = runTest(dispatcher) {
        val viewModel = SafetyViewModel(FakeSafetyRepository())
        runCurrent()

        viewModel.emergencyStop("test stop")
        advanceUntilIdle()
        val eventId = viewModel.uiState.value.activeIncidents.single().id

        viewModel.acknowledge(eventId)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEmergencyActive)
        assertTrue(viewModel.uiState.value.activeIncidents.isEmpty())
        assertNotNull(viewModel.uiState.value.incidents.singleOrNull { it.acknowledged })
    }

    @Test
    fun repositoryFailureIsShownInUiState() = runTest(dispatcher) {
        val delegate = FakeSafetyRepository()
        val repository = object : SafetyRepository {
            override fun observeSafetyEvents(): Flow<List<SafetyEvent>> =
                delegate.observeSafetyEvents()

            override suspend fun evaluateAndExecute(
                command: RoboCommand,
                context: SafetyContext
            ): SafetyDecision = delegate.evaluateAndExecute(command, context)

            override suspend fun emergencyStop(reason: String): CommandResult =
                delegate.emergencyStop(reason)

            override suspend fun safePosition(reason: String): CommandResult {
                throw IllegalStateException("device unavailable")
            }

            override suspend fun acknowledge(eventId: String) = delegate.acknowledge(eventId)
        }
        val viewModel = SafetyViewModel(repository)
        runCurrent()

        viewModel.safePosition()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("device unavailable", viewModel.uiState.value.error)
    }
}
