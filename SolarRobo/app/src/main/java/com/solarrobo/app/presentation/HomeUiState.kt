package com.solarrobo.app.presentation

import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.SafetyLevel

data class HomeUiState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = true,
    val connectionText: String = "Connected / Simulator",
    val mode: RoboMode = RoboMode.NORMAL,
    val panelAngleDeg: Float = 45.0f,
    val generationWatts: Float = 320.0f,
    val batteryPercent: Float = 94.0f,
    val environmentText: String = "Sunny / Normal",
    val safetyLevel: SafetyLevel = SafetyLevel.NORMAL,
    val latestMessage: String = "Solar generation is stable.",
    val errorMessage: String? = null
)
