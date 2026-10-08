package com.solarrobo.feature.energy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.energy.domain.CalculateEnergyMetrics
import com.solarrobo.feature.energy.domain.EnergyRepository
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
class EnergyViewModel @Inject constructor(
    private val repository: EnergyRepository,
    private val calculateMetrics: CalculateEnergyMetrics
) : ViewModel() {

    private val refreshTriggers = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val uiState: StateFlow<EnergyUiState> = refreshTriggers
        .onStart { emit(Unit) }
        .flatMapLatest {
            combine(
                repository.getEnergySnapshot(),
                repository.getEnergyHistory(),
                repository.getErrorMessage()
            ) { snapshot, history, errorMessage ->
                if (errorMessage != null) {
                    EnergyUiState.Error(message = errorMessage)
                } else {
                    val metrics = calculateMetrics(snapshot, history)
                    EnergyUiState.Success(
                        snapshot = snapshot,
                        history = history,
                        metrics = metrics
                    )
                }
            }.onStart {
                emit(EnergyUiState.Loading)
            }.catch { error ->
                emit(EnergyUiState.Error(message = error.message ?: "Failed to load energy state."))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EnergyUiState.Loading
        )

    fun refresh() {
        viewModelScope.launch {
            repository.refresh()
            refreshTriggers.tryEmit(Unit)
        }
    }
}
