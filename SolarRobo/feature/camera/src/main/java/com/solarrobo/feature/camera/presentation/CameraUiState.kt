package com.solarrobo.feature.camera.presentation

import com.solarrobo.feature.camera.domain.CameraAnalysisState
import com.solarrobo.feature.camera.domain.CameraConnectionState
import com.solarrobo.feature.camera.domain.CameraFrame

data class CameraUiState(
    val connectionState: CameraConnectionState = CameraConnectionState.DISCONNECTED,
    val permissionGranted: Boolean = false,
    val lastCapturedSnapshot: CameraFrame? = null,
    val analysis: CameraAnalysisState = CameraAnalysisState(),
    val errorMessage: String? = null
)
