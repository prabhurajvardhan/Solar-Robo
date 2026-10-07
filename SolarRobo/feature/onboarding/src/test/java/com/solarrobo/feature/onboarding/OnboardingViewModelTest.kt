package com.solarrobo.feature.onboarding

import com.solarrobo.feature.onboarding.domain.DeviceCandidate
import com.solarrobo.feature.onboarding.mock.FakeOnboardingRepository
import com.solarrobo.feature.onboarding.presentation.OnboardingStep
import com.solarrobo.feature.onboarding.presentation.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = OnboardingViewModel(FakeOnboardingRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isDiscoveryStep() {
        assertEquals(OnboardingStep.DISCOVERY, viewModel.uiState.value.step)
        assertEquals(false, viewModel.uiState.value.isScanning)
    }

    @Test
    fun startScanning_emitsCandidates() = runTest {
        viewModel.startScanning()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isScanning)
        assertEquals(2, viewModel.uiState.value.devices.size)
        assertEquals("MOCK-001", viewModel.uiState.value.devices.first().id)
    }

    @Test
    fun selectDevice_updatesStepToConfiguration() {
        val candidate = DeviceCandidate("TEST-1", "Tracker Alpha", -50)
        viewModel.selectDevice(candidate)

        assertEquals(OnboardingStep.CONFIGURATION, viewModel.uiState.value.step)
        assertEquals(candidate, viewModel.uiState.value.selectedDevice)
    }

    @Test
    fun confirmOnboarding_successTransitionsToCompleted() = runTest {
        val candidate = DeviceCandidate("TEST-1", "Tracker Alpha", -50)
        viewModel.selectDevice(candidate)
        viewModel.confirmOnboarding("My Solar Robo", "HomeWiFi", "pass123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(OnboardingStep.COMPLETED, viewModel.uiState.value.step)
        assertEquals(false, viewModel.uiState.value.isConnecting)
    }

    @Test
    fun confirmOnboarding_failureSetsErrorMessage() = runTest {
        val failRepo = FakeOnboardingRepository(shouldFailConnection = true)
        val failVm = OnboardingViewModel(failRepo)

        val candidate = DeviceCandidate("FAIL-1", "Broken Device", -95)
        failVm.selectDevice(candidate)
        failVm.confirmOnboarding("My Solar Robo", "HomeWiFi", "pass123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(failVm.uiState.value.errorMessage)
        assertEquals(false, failVm.uiState.value.isConnecting)
    }
}
