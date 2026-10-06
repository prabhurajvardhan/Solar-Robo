package com.solarrobo.feature.camera.presentation

import com.solarrobo.feature.camera.domain.CameraConnectionState
import com.solarrobo.feature.camera.domain.CameraFrame

data class CameraUiState(
    val connectionState: CameraConnectionState = CameraConnectionState.DISCONNECTED,
    val permissionGranted: Boolean = false,
    val lastCapturedSnapshot: CameraFrame? = null,
    val fps: Int = 0,
    val errorMessage: String? = null
)
