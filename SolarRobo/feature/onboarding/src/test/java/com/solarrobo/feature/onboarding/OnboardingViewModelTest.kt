package com.solarrobo.feature.onboarding

import com.solarrobo.feature.onboarding.mock.FakeOnboardingRepository
import com.solarrobo.feature.onboarding.presentation.OnboardingStep
import com.solarrobo.feature.onboarding.presentation.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun initialStateStartsWithSolarSafety() {
        val viewModel = OnboardingViewModel(FakeOnboardingRepository())
        assertEquals(OnboardingStep.SOLAR_SAFETY, viewModel.uiState.value.step)
    }

    @Test
    fun continueMovesToWifiSetupWithoutInventingScanResults() {
        val viewModel = OnboardingViewModel(FakeOnboardingRepository())
        viewModel.continueToWifiSetup()
        assertEquals(OnboardingStep.WIFI_LINK, viewModel.uiState.value.step)
    }

    @Test
    fun verifiedLocalLinkTransitionsToCompleted() = runTest(testDispatcher) {
        val viewModel = OnboardingViewModel(FakeOnboardingRepository())
        viewModel.continueToWifiSetup()
        viewModel.verifyWifiLink()
        advanceUntilIdle()
        assertEquals(OnboardingStep.COMPLETED, viewModel.uiState.value.step)
        assertEquals(false, viewModel.uiState.value.isConnecting)
    }

    @Test
    fun failedStatusVerificationReturnsToWifiStepAndCanRetry() = runTest(testDispatcher) {
        val viewModel = OnboardingViewModel(FakeOnboardingRepository(shouldFailConnection = true))
        viewModel.continueToWifiSetup()
        viewModel.verifyWifiLink()
        advanceUntilIdle()
        assertEquals(OnboardingStep.WIFI_LINK, viewModel.uiState.value.step)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isConnecting)
    }
}
