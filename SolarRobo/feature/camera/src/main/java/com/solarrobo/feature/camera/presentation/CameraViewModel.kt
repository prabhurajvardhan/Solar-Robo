package com.solarrobo.feature.camera.presentation

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.camera.domain.CameraConnectionState
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

    fun bindPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        repository.bindPreview(previewView, lifecycleOwner)
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                permissionGranted = granted,
                errorMessage = if (!granted) {
                    "Camera permission was denied. Enable it in Settings to use the Robo camera."
                } else {
                    null
                }
            )
        }
    }

    fun startCamera() {
        if (!_uiState.value.permissionGranted) {
            _uiState.update { it.copy(errorMessage = "Camera permission required") }
            return
        }

        viewModelScope.launch {
            val result = repository.startStream()
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        connectionState = CameraConnectionState.ERROR,
                        errorMessage = result.exceptionOrNull()?.message ?: "Unable to start camera"
                    )
                }
            } else {
                _uiState.update { it.copy(connectionState = CameraConnectionState.STREAMING, errorMessage = null) }
            }
        }
    }

    fun stopCamera() {
        viewModelScope.launch {
            repository.stopStream()
            _uiState.update { it.copy(connectionState = CameraConnectionState.DISCONNECTED, errorMessage = null) }
        }
    }

    fun takeSnapshot() {
        viewModelScope.launch {
            val result = repository.captureSnapshot()
            result.onSuccess { frame ->
                _uiState.update { it.copy(lastCapturedSnapshot = frame, errorMessage = null) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(connectionState = CameraConnectionState.ERROR, errorMessage = throwable.message ?: "Snapshot failed") }
            }
        }
    }
}
