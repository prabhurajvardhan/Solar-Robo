package com.solarrobo.feature.onboarding.mock

import com.solarrobo.feature.onboarding.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeOnboardingRepository(
    private val shouldFailConnection: Boolean = false
) : OnboardingRepository {

    override fun scanDevices(): Flow<List<DeviceCandidate>> = flowOf(
        listOf(
            DeviceCandidate("MOCK-001", "Simulated Solar Robo 1", -45),
            DeviceCandidate("MOCK-002", "Simulated Backyard Tracker", -68)
        )
    )

    override suspend fun connectDevice(deviceId: String): Result<Unit> {
        return if (shouldFailConnection) {
            Result.failure(Exception("GATT timeout connecting to $deviceId"))
        } else {
            Result.success(Unit)
        }
    }

    override suspend fun saveConfig(config: DeviceConfig): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun complete(config: DeviceConfig): OnboardingResult {
        val connectRes = connectDevice(config.deviceId)
        return if (connectRes.isFailure) {
            OnboardingResult.Failure("Connection failed to ${config.deviceId}")
        } else {
            OnboardingResult.Success(config.deviceId)
        }
    }
}
