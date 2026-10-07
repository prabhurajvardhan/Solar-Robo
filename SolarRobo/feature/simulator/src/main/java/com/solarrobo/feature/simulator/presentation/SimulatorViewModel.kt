package com.solarrobo.feature.simulator.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.simulator.domain.Scenario
import com.solarrobo.feature.simulator.domain.SimulatorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SimulatorViewModel @Inject constructor(
    private val repository: SimulatorRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SimulatorUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getRoboSnapshot(),
                repository.getEnergySnapshot(),
                repository.getEnvironmentSnapshot(),
                repository.getDeviceHealth(),
                repository.getSafetyEvents()
            ) { robo, energy, environment, health, events ->
                _uiState.update {
                    it.copy(
                        robo = robo,
                        energy = energy,
                        environment = environment,
                        deviceHealth = health,
                        safetyEvents = events,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.catch { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Simulator data unavailable.")
                }
            }.collect { }
        }
    }

    fun selectScenario(scenario: Scenario) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.applyScenario(scenario) }
                .onSuccess {
                    _uiState.update { it.copy(currentScenario = scenario, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Scenario could not be applied.")
                    }
                }
        }
    }

    fun advanceTime(deltaMillis: Long) {
        viewModelScope.launch {
            runCatching { repository.tick(deltaMillis) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Simulated time could not advance.")
                    }
                }
        }
    }

    fun injectFault(code: String, message: String) {
        viewModelScope.launch {
            runCatching { repository.injectFault(code, message) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Fault could not be injected.")
                    }
                }
        }
    }

    fun triggerMotorJamFault() {
        injectFault("MOTOR_STALL_01", "Simulated motor stall.")
    }
}