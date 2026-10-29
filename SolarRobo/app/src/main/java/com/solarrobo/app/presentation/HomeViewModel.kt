package com.solarrobo.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.core.ai.AiEngine
import com.solarrobo.core.ai.AiRequest
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.core.device.RoboDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val roboDevice: RoboDevice,
    private val aiEngine: AiEngine
) : ViewModel() {
    private val latestMessage = MutableStateFlow("Tracker readings are not available yet.")

    init { refreshAiSummary() }

    val uiState: StateFlow<HomeUiState> = combine(roboDevice.observeSnapshot(), latestMessage) { robo: RoboSnapshot, assistantMessage ->
        HomeUiState(
            isLoading = false,
            isConnected = robo.connected,
            connectionText = if (robo.connected) "SolarTracker Wi-Fi connected" else "Offline · no telemetry available",
            mode = robo.mode,
            // Angle, output and battery are surfaced only after TrackerEngine supplies a verified estimate/source.
            panelAngleDeg = null,
            generationWatts = null,
            batteryPercent = null,
            environmentText = "Not measured",
            safetyLevel = if (robo.mode == RoboMode.FAULT) SafetyLevel.FAULT else SafetyLevel.NORMAL,
            latestMessage = assistantMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(isLoading = true))

    fun refreshAiSummary() {
        viewModelScope.launch {
            latestMessage.value = aiEngine.generate(
                AiRequest("Explain safe setup for a sensorless solar tracker. Do not assume telemetry or issue motor commands.")
            ).text
        }
    }
}
