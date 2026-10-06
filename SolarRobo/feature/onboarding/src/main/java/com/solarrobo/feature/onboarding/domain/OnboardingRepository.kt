package com.solarrobo.feature.onboarding.domain

import kotlinx.coroutines.flow.Flow

data class DeviceCandidate(
    val id: String,
    val name: String,
    val rssi: Int,
    val isPaired: Boolean = false
)

data class DeviceConfig(
    val deviceId: String,
    val displayName: String,
    val wifiSsid: String,
    val wifiPass: String
)

sealed interface OnboardingResult {
    data class Success(val deviceId: String) : OnboardingResult
    data class Failure(val message: String) : OnboardingResult
}

interface OnboardingRepository {
    fun scanDevices(): Flow<List<DeviceCandidate>>
    suspend fun connectDevice(deviceId: String): Result<Unit>
    suspend fun saveConfig(config: DeviceConfig): Result<Unit>
    suspend fun complete(config: DeviceConfig): OnboardingResult
}
