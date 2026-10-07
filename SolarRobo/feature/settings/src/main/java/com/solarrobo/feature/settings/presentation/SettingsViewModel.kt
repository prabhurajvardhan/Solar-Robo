package com.solarrobo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.settings.domain.SettingsPatch
import com.solarrobo.feature.settings.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState = mutableUiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        retry()
    }

    fun update(patch: SettingsPatch) {
        viewModelScope.launch {
            repository.update(patch)
                .onSuccess {
                    mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
                }
                .onFailure { error ->
                    mutableUiState.value = mutableUiState.value.copy(
                        errorMessage = error.message ?: "Settings could not be saved."
                    )
                }
        }
    }

    fun reset() {
        viewModelScope.launch {
            repository.reset()
                .onSuccess {
                    mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
                }
                .onFailure { error ->
                    mutableUiState.value = mutableUiState.value.copy(
                        errorMessage = error.message ?: "Settings could not be reset."
                    )
                }
        }
    }

    fun retry() {
        loadJob?.cancel()
        mutableUiState.value = SettingsUiState(isLoading = true)
        loadJob = viewModelScope.launch {
            repository.load()
                .catch { error ->
                    if (error is IOException || error is IllegalArgumentException) {
                        mutableUiState.value = SettingsUiState(
                            isOffline = true,
                            errorMessage = error.message ?: "Settings could not be loaded."
                        )
                    } else {
                        throw error
                    }
                }
                .collect { settings ->
                    mutableUiState.value = SettingsUiState(
                        settings = settings,
                        isOffline = true
                    )
                }
        }
    }
}
