package com.solarrobo.app.presentation

import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.SafetyLevel

data class HomeUiState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val connectionText: String = "Offline",
    val mode: RoboMode = RoboMode.SAFE,
    val panelAngleDeg: Float? = null,
    val generationWatts: Float? = null,
    val batteryPercent: Float? = null,
    val environmentText: String = "Not measured",
    val safetyLevel: SafetyLevel = SafetyLevel.NORMAL,
    val latestMessage: String = "Connect the tracker and complete setup to begin.",
    val errorMessage: String? = null
)
