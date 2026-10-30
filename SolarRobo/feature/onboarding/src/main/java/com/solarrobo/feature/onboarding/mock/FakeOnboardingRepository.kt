package com.solarrobo.feature.onboarding.mock

import com.solarrobo.feature.onboarding.domain.OnboardingRepository
import com.solarrobo.feature.onboarding.domain.OnboardingResult

class FakeOnboardingRepository(
    private val shouldFailConnection: Boolean = false
) : OnboardingRepository {
    override suspend fun verifyWifiLink(): OnboardingResult = if (shouldFailConnection) {
        OnboardingResult.Failure("Simulated status verification failure.")
    } else {
        OnboardingResult.Success
    }
}
