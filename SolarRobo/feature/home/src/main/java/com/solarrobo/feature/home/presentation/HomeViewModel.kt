package com.solarrobo.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.home.domain.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private data class HomeData(
        val robo: com.solarrobo.core.contracts.RoboSnapshot,
        val energy: com.solarrobo.core.contracts.EnergySnapshot,
        val safetyLevel: com.solarrobo.core.contracts.SafetyLevel,
        val alerts: List<com.solarrobo.core.contracts.SafetyEvent>,
        val message: String
    )

    private val refreshTriggers = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val uiState: StateFlow<HomeUiState> = refreshTriggers
        .onStart { emit(Unit) }
        .flatMapLatest {
            combine(
                repository.getRoboSnapshot(),
                repository.getEnergySnapshot(),
                repository.getSafetyLevel(),
                repository.getActiveSafetyEvents(),
                repository.getLatestRoboMessage()
            ) { robo, energy, safety, alerts, message ->
                HomeData(robo, energy, safety, alerts, message)
            }.combine(repository.getErrorMessage()) { data, error ->
                if (error != null) {
                    HomeUiState.Error(message = error)
                } else {
                    HomeUiState.Success(
                        robo = data.robo,
                        energy = data.energy,
                        safetyLevel = data.safetyLevel,
                        activeAlerts = data.alerts.filter { !it.acknowledged },
                        statusMessage = data.message
                    )
                }
            }.onStart {
                emit(HomeUiState.Loading)
            }.catch { err ->
                emit(HomeUiState.Error(message = err.message ?: "Failed to load command center."))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading
        )

    fun refresh() {
        viewModelScope.launch {
            repository.refresh()
            refreshTriggers.tryEmit(Unit)
        }
    }
}
