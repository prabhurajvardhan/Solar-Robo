package com.solarrobo.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.onboarding.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun startScanning() {
        _uiState.update { it.copy(isScanning = true, errorMessage = null) }
        viewModelScope.launch {
            repository.scanDevices()
                .catch { err ->
                    _uiState.update { it.copy(isScanning = false, errorMessage = err.message) }
                }
                .collect { list ->
                    _uiState.update { it.copy(isScanning = false, devices = list) }
                }
        }
    }

    fun selectDevice(candidate: DeviceCandidate) {
        _uiState.update {
            it.copy(
                selectedDevice = candidate,
                step = OnboardingStep.CONFIGURATION
            )
        }
    }

    fun confirmOnboarding(name: String, wifiSsid: String, wifiPass: String) {
        val device = _uiState.value.selectedDevice ?: return
        _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
        viewModelScope.launch {
            val config = DeviceConfig(device.id, name, wifiSsid, wifiPass)
            when (val res = repository.complete(config)) {
                is OnboardingResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            step = OnboardingStep.COMPLETED
                        )
                    }
                }
                is OnboardingResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            errorMessage = res.message
                        )
                    }
                }
            }
        }
    }
}
