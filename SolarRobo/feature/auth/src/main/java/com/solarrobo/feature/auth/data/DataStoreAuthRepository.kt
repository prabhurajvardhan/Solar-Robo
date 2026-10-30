package com.solarrobo.feature.auth.data

import com.solarrobo.core.database.DatabaseProvider
import com.solarrobo.core.storage.SecurePreferencesStore
import com.solarrobo.feature.auth.domain.AuthClock
import com.solarrobo.feature.auth.domain.AuthProfile
import com.solarrobo.feature.auth.domain.AuthRepository
import com.solarrobo.feature.auth.domain.PinHasher
import com.solarrobo.feature.auth.domain.PinVerification
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreAuthRepository @Inject constructor(
    private val store: SecurePreferencesStore,
    private val database: DatabaseProvider,
    private val hasher: PinHasher,
    private val clock: AuthClock
) : AuthRepository {
    override val profile: Flow<AuthProfile?> = store.preferences.map { values ->
        val name = values[KEY_NAME]
        if (name.isNullOrBlank() || values[KEY_SALT].isNullOrBlank() || values[KEY_HASH].isNullOrBlank()) null
        else AuthProfile(
            displayName = name,
            biometricEnabled = values[KEY_BIOMETRIC] == "true",
            autoLockMinutes = values[KEY_AUTO_LOCK]?.toIntOrNull()?.takeIf { it in AUTO_LOCK_CHOICES } ?: DEFAULT_AUTO_LOCK
        )
    }

    override suspend fun createProfile(displayName: String, pin: String, confirmation: String, biometricEnabled: Boolean) {
        val cleanName = displayName.trim()
        require(cleanName.isNotEmpty()) { "Enter a display name." }
        require(pin == confirmation) { "PINs do not match." }
        require(PIN_INPUT.matches(pin)) { "PIN must contain exactly 6 digits." }
        require(profile.first() == null) { "A local profile already exists." }
        val verifier = hasher.create(pin)
        store.update { current -> current + mapOf(
            KEY_NAME to cleanName,
            KEY_SALT to verifier.saltBase64,
            KEY_HASH to verifier.hashBase64,
            KEY_BIOMETRIC to biometricEnabled.toString(),
            KEY_AUTO_LOCK to DEFAULT_AUTO_LOCK.toString(),
            KEY_FAILURES to "0",
            KEY_LOCK_UNTIL to "0"
        ) }
    }

    override suspend fun verifyPin(pin: String): PinVerification {
        val values = store.preferences.first()
        val now = clock.nowMillis()
        val lockUntil = values[KEY_LOCK_UNTIL]?.toLongOrNull() ?: 0L
        if (lockUntil > now) return PinVerification.Locked(lockUntil - now)
        val salt = values[KEY_SALT] ?: return PinVerification.Rejected(0)
        val expected = values[KEY_HASH] ?: return PinVerification.Rejected(0)
        if (hasher.verify(pin, salt, expected)) {
            store.update { it + (KEY_FAILURES to "0") + (KEY_LOCK_UNTIL to "0") }
            return PinVerification.Accepted
        }
        val failures = (values[KEY_FAILURES]?.toIntOrNull() ?: 0) + 1
        val lockMillis = if (failures >= LOCKOUT_THRESHOLD) {
            val exponent = (failures - LOCKOUT_THRESHOLD).coerceAtMost(MAX_LOCKOUT_EXPONENT)
            (BASE_LOCKOUT_MS * (1L shl exponent)).coerceAtMost(MAX_LOCKOUT_MS)
        } else 0L
        val until = if (lockMillis > 0) now + lockMillis else 0L
        store.update { it + (KEY_FAILURES to failures.toString()) + (KEY_LOCK_UNTIL to until.toString()) }
        return if (lockMillis > 0) PinVerification.Locked(lockMillis)
        else PinVerification.Rejected((LOCKOUT_THRESHOLD - failures).coerceAtLeast(0))
    }

    override suspend fun remainingLockoutMillis(): Long {
        val lockUntil = store.preferences.first()[KEY_LOCK_UNTIL]?.toLongOrNull() ?: 0L
        return (lockUntil - clock.nowMillis()).coerceAtLeast(0L)
    }

    override suspend fun changePin(currentPin: String, newPin: String, confirmation: String) {
        require(verifyPin(currentPin) == PinVerification.Accepted) { "Current PIN is incorrect or temporarily locked." }
        require(newPin == confirmation) { "New PINs do not match." }
        val verifier = hasher.create(newPin)
        store.update { it + mapOf(KEY_SALT to verifier.saltBase64, KEY_HASH to verifier.hashBase64) }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        store.update { it + (KEY_BIOMETRIC to enabled.toString()) }
    }

    override suspend fun setAutoLockMinutes(minutes: Int) {
        require(minutes in AUTO_LOCK_CHOICES) { "Auto-lock must be 1, 5, or 15 minutes." }
        store.update { it + (KEY_AUTO_LOCK to minutes.toString()) }
    }

    override suspend fun resetProfile(pin: String): PinVerification {
        val verification = verifyPin(pin)
        if (verification == PinVerification.Accepted) {
            database.clearAllTables()
            store.update { emptyMap() }
        }
        return verification
    }

    private companion object {
        const val KEY_NAME = "auth.profile.name"
        const val KEY_SALT = "auth.pin.salt"
        const val KEY_HASH = "auth.pin.hash"
        const val KEY_BIOMETRIC = "auth.biometric.enabled"
        const val KEY_AUTO_LOCK = "auth.auto_lock.minutes"
        const val KEY_FAILURES = "auth.pin.failed_attempts"
        const val KEY_LOCK_UNTIL = "auth.pin.lock_until_ms"
        const val LOCKOUT_THRESHOLD = 5
        const val BASE_LOCKOUT_MS = 30_000L
        const val MAX_LOCKOUT_MS = 15 * 60_000L
        const val MAX_LOCKOUT_EXPONENT = 10
        const val DEFAULT_AUTO_LOCK = 5
        val AUTO_LOCK_CHOICES = setOf(1, 5, 15)
        val PIN_INPUT = Regex("^[0-9]{6}$")
    }
}
