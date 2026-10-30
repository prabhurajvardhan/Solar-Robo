package com.solarrobo.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.onboarding.domain.OnboardingRepository
import com.solarrobo.feature.onboarding.domain.OnboardingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: OnboardingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun continueToWifiSetup() {
        _uiState.update { it.copy(step = OnboardingStep.WIFI_LINK, errorMessage = null) }
    }

    fun verifyWifiLink() {
        if (_uiState.value.isConnecting) return
        _uiState.update { it.copy(isConnecting = true, step = OnboardingStep.VERIFICATION, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.verifyWifiLink()) {
                OnboardingResult.Success -> _uiState.update {
                    it.copy(isConnecting = false, step = OnboardingStep.COMPLETED)
                }
                is OnboardingResult.Failure -> _uiState.update {
                    it.copy(isConnecting = false, step = OnboardingStep.WIFI_LINK, errorMessage = result.message)
                }
            }
        }
    }
}
