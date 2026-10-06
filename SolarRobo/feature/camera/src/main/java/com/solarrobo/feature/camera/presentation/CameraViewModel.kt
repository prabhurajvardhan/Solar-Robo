package com.solarrobo.feature.camera.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.camera.domain.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: CameraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeConnectionState().collect { connectionState ->
                _uiState.update { it.copy(connectionState = connectionState) }
            }
        }
    }

    fun startCamera() {
        viewModelScope.launch {
            val result = repository.startStream()
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            } else {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    fun stopCamera() {
        viewModelScope.launch {
            repository.stopStream()
            _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun takeSnapshot() {
        viewModelScope.launch {
            val result = repository.captureSnapshot()
            result.onSuccess { frame ->
                _uiState.update { it.copy(lastCapturedSnapshot = frame, errorMessage = null) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(errorMessage = throwable.message) }
            }
        }
    }
}
