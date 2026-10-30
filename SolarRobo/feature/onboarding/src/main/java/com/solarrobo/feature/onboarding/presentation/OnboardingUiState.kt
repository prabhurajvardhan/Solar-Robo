package com.solarrobo.feature.onboarding.presentation

enum class OnboardingStep {
    SOLAR_SAFETY,
    WIFI_LINK,
    VERIFICATION,
    COMPLETED
}

data class OnboardingUiState(
    val isConnecting: Boolean = false,
    val step: OnboardingStep = OnboardingStep.SOLAR_SAFETY,
    val errorMessage: String? = null
)
