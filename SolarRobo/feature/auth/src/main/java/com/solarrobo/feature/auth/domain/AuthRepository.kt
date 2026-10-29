package com.solarrobo.feature.auth.domain

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val profile: Flow<AuthProfile?>
    suspend fun createProfile(displayName: String, pin: String, confirmation: String, biometricEnabled: Boolean)
    suspend fun verifyPin(pin: String): PinVerification
    suspend fun remainingLockoutMillis(): Long
    suspend fun changePin(currentPin: String, newPin: String, confirmation: String)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setAutoLockMinutes(minutes: Int)
    suspend fun resetProfile(pin: String): PinVerification
}

data class AuthProfile(
    val displayName: String,
    val biometricEnabled: Boolean,
    val autoLockMinutes: Int
)

sealed interface PinVerification {
    data object Accepted : PinVerification
    data class Rejected(val attemptsRemaining: Int) : PinVerification
    data class Locked(val retryAfterMillis: Long) : PinVerification
}

fun interface AuthClock { fun nowMillis(): Long }

class SystemAuthClock @javax.inject.Inject constructor() : AuthClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
