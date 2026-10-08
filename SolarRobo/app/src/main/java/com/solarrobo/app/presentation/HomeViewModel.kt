package com.solarrobo.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.core.ai.AiEngine
import com.solarrobo.core.ai.AiRequest
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.core.simulator.SimulatorEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val simulatorEngine: SimulatorEngine,
    private val aiEngine: AiEngine
) : ViewModel() {

    private val _latestAiMessage = MutableStateFlow("Solar generation is stable.")

    init {
        // Query AI engine for initial summary message
        viewModelScope.launch {
            val response = aiEngine.generate(AiRequest("How is the Robo?"))
            _latestAiMessage.value = response.text
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        simulatorEngine.observeRoboSnapshot(),
        simulatorEngine.observeEnergySnapshot(),
        simulatorEngine.observeEnvironmentSnapshot(),
        simulatorEngine.observeSafetyEvents(),
        _latestAiMessage
    ) { robo, energy, env, safetyEvents, aiMsg ->
        val latestSafety = safetyEvents.lastOrNull()?.level ?: SafetyLevel.NORMAL
        val envDescription = when {
            env.rainDetected -> "Rain / Protecting"
            env.windSpeedMps > 15f -> "High Wind (${env.windSpeedMps} m/s)"
            env.lightLux < 10000f -> "Low Light / Overcast"
            else -> "Sunny / Normal (${env.temperatureC}°C)"
        }

        HomeUiState(
            isLoading = false,
            isConnected = robo.connected,
            connectionText = if (robo.connected) "Connected / Simulator" else "Disconnected",
            mode = robo.mode,
            panelAngleDeg = robo.panelAngleDeg,
            generationWatts = energy.generatedWatts,
            batteryPercent = energy.batteryPercent,
            environmentText = envDescription,
            safetyLevel = latestSafety,
            latestMessage = aiMsg,
            errorMessage = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun refreshAiSummary() {
        viewModelScope.launch {
            val response = aiEngine.generate(AiRequest("How is the Robo?"))
            _latestAiMessage.value = response.text
        }
    }
}
