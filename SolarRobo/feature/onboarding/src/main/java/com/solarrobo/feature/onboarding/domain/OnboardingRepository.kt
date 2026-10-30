package com.solarrobo.feature.onboarding.domain

sealed interface OnboardingResult {
    data object Success : OnboardingResult
    data class Failure(val message: String) : OnboardingResult
}

/** Requests the configured local AP through Android and verifies the ESP32 status endpoint. */
interface OnboardingRepository {
    suspend fun verifyWifiLink(): OnboardingResult
}
