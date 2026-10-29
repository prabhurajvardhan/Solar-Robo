package com.solarrobo.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.core.contracts.AccessMode
import com.solarrobo.feature.auth.domain.AuthProfile
import com.solarrobo.feature.auth.domain.AuthRepository
import com.solarrobo.feature.auth.domain.AuthSession
import com.solarrobo.feature.auth.domain.PinVerification
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val session: AuthSession
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AuthUiState(isLoading = true))
    val uiState = mutableUiState.asStateFlow()
    val accessMode = session.accessMode
    val isLocked = session.isLocked
    private var autoLockJob: Job? = null
    private var countdownJob: Job? = null
    private var backgroundAt: Long? = null

    init {
        viewModelScope.launch {
            repository.profile
                .catch { error ->
                    mutableUiState.value = mutableUiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Local profile could not be read."
                    )
                }
                .collect { profile ->
                    mutableUiState.value = mutableUiState.value.copy(isLoading = false, profile = profile)
                    if (profile != null) {
                        val remaining = repository.remainingLockoutMillis()
                        if (remaining > 0L) startLockoutCountdown(remaining)
                    }
                }
        }
    }

    fun createProfile(name: String, pin: String, confirmation: String, biometric: Boolean) = runAction {
        repository.createProfile(name, pin, confirmation, biometric)
        session.unlockOperator()
        mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
    }

    fun unlockWithPin(pin: String) {
        viewModelScope.launch {
            when (val result = repository.verifyPin(pin)) {
                PinVerification.Accepted -> {
                    session.unlockOperator()
                    countdownJob?.cancel()
                    mutableUiState.value = mutableUiState.value.copy(errorMessage = null, pinMessage = null, lockoutRemainingMs = 0L)
                }
                is PinVerification.Rejected -> mutableUiState.value = mutableUiState.value.copy(
                    errorMessage = "PIN is incorrect. ${result.attemptsRemaining} attempts remain before a timed lockout."
                )
                is PinVerification.Locked -> startLockoutCountdown(result.retryAfterMillis)
            }
        }
    }

    fun unlockWithBiometric() {
        if (mutableUiState.value.profile?.biometricEnabled == true) session.unlockOperator()
    }

    fun enterViewerMode() = session.enterViewerMode()
    fun lockNow() = session.lock()

    fun onAppBackgrounded() {
        backgroundAt = System.currentTimeMillis()
        autoLockJob?.cancel()
        val timeout = mutableUiState.value.profile?.autoLockMinutes ?: return
        if (session.accessMode.value != AccessMode.OPERATOR || session.isLocked.value) return
        autoLockJob = viewModelScope.launch {
            delay(timeout * 60_000L)
            session.lock()
        }
    }

    fun onAppForegrounded() {
        val wasBackgroundedAt = backgroundAt
        backgroundAt = null
        autoLockJob?.cancel()
        val timeout = mutableUiState.value.profile?.autoLockMinutes ?: 5
        if (wasBackgroundedAt != null && System.currentTimeMillis() - wasBackgroundedAt >= timeout * 60_000L) {
            session.lock()
        }
    }

    fun changePin(current: String, replacement: String, confirmation: String) = runAction {
        repository.changePin(current, replacement, confirmation)
        mutableUiState.value = mutableUiState.value.copy(errorMessage = null, pinMessage = "PIN changed.")
    }

    fun setBiometricEnabled(enabled: Boolean) = runAction {
        repository.setBiometricEnabled(enabled)
        mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
    }

    fun setAutoLockMinutes(minutes: Int) = runAction {
        repository.setAutoLockMinutes(minutes)
        mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
    }

    fun resetProfile(pin: String) {
        viewModelScope.launch {
            when (val result = repository.resetProfile(pin)) {
                PinVerification.Accepted -> {
                    session.lock()
                    mutableUiState.value = mutableUiState.value.copy(errorMessage = null, pinMessage = "Local profile and stored data were reset.")
                }
                is PinVerification.Rejected -> mutableUiState.value = mutableUiState.value.copy(errorMessage = "PIN is incorrect.")
                is PinVerification.Locked -> startLockoutCountdown(result.retryAfterMillis)
            }
        }
    }

    private fun startLockoutCountdown(remaining: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var seconds = (remaining + 999L) / 1_000L
            while (seconds > 0L) {
                mutableUiState.value = mutableUiState.value.copy(
                    errorMessage = "Too many incorrect attempts. Try again in $seconds seconds.",
                    lockoutRemainingMs = seconds * 1_000L
                )
                delay(1_000L)
                seconds--
            }
            mutableUiState.value = mutableUiState.value.copy(lockoutRemainingMs = 0L, errorMessage = null)
        }
    }

    private fun runAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }.onFailure { error ->
                mutableUiState.value = mutableUiState.value.copy(
                    errorMessage = error.message ?: "Security settings could not be saved."
                )
            }
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val profile: AuthProfile? = null,
    val errorMessage: String? = null,
    val pinMessage: String? = null,
    val lockoutRemainingMs: Long = 0L
)
