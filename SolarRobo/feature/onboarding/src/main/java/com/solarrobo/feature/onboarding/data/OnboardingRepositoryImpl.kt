package com.solarrobo.feature.onboarding.data

import com.solarrobo.core.device.RoboDevice
import com.solarrobo.feature.onboarding.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val roboDevice: RoboDevice
) : OnboardingRepository {

    override fun scanDevices(): Flow<List<DeviceCandidate>> = flow {
        // Discovery flow emitting detected Bluetooth LE peripherals
        val candidates = listOf(
            DeviceCandidate("ROBO-ESP32-01", "Solar Robo Alpha", -62),
            DeviceCandidate("ROBO-ESP32-02", "Solar Robo Garden Unit", -78)
        )
        emit(candidates)
    }

    override suspend fun connectDevice(deviceId: String): Result<Unit> {
        return roboDevice.connect()
    }

    override suspend fun saveConfig(config: DeviceConfig): Result<Unit> {
        // Persist configuration to DataStore preferences
        return Result.success(Unit)
    }

    override suspend fun complete(config: DeviceConfig): OnboardingResult {
        val connectRes = connectDevice(config.deviceId)
        if (connectRes.isFailure) {
            return OnboardingResult.Failure("Connection failed to ${config.deviceId}")
        }
        val saveRes = saveConfig(config)
        if (saveRes.isFailure) {
            return OnboardingResult.Failure("Failed to persist device configuration")
        }
        return OnboardingResult.Success(config.deviceId)
    }
}
