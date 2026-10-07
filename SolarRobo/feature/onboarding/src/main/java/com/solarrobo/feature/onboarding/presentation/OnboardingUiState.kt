package com.solarrobo.feature.onboarding.presentation

import com.solarrobo.feature.onboarding.domain.DeviceCandidate

enum class OnboardingStep {
    DISCOVERY,
    CONFIGURATION,
    VERIFICATION,
    COMPLETED
}

data class OnboardingUiState(
    val isScanning: Boolean = false,
    val devices: List<DeviceCandidate> = emptyList(),
    val selectedDevice: DeviceCandidate? = null,
    val isConnecting: Boolean = false,
    val step: OnboardingStep = OnboardingStep.DISCOVERY,
    val errorMessage: String? = null
)
