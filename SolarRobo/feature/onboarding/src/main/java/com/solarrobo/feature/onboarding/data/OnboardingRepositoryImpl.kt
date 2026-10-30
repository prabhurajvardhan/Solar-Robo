package com.solarrobo.feature.onboarding.data

import com.solarrobo.core.device.RoboDevice
import com.solarrobo.feature.onboarding.domain.OnboardingRepository
import com.solarrobo.feature.onboarding.domain.OnboardingResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val roboDevice: RoboDevice
) : OnboardingRepository {
    override suspend fun verifyWifiLink(): OnboardingResult {
        val connection = roboDevice.connect()
        if (connection.isFailure) {
            val reason = connection.exceptionOrNull()?.localizedMessage
            return OnboardingResult.Failure(reason ?: "Android could not join the SolarTracker Wi-Fi network.")
        }
        val snapshot = runCatching { roboDevice.getSnapshot() }.getOrNull()
        return if (snapshot?.connected == true) {
            OnboardingResult.Success
        } else {
            OnboardingResult.Failure("The Wi-Fi request completed, but the ESP32 status check was not verified. Check the AP and try again.")
        }
    }
}
