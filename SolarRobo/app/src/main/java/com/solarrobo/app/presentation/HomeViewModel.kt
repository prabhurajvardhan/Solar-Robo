package com.solarrobo.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.app.di.TrackerRuntimeHolder
import com.solarrobo.core.ai.AiEngine
import com.solarrobo.core.ai.AiRequest
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.LinkState
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.core.contracts.TrackerLogSink
import com.solarrobo.core.contracts.TrackerSnapshot
import com.solarrobo.core.device.RoboDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val roboDevice: RoboDevice,
    private val aiEngine: AiEngine,
    runtimeHolder: TrackerRuntimeHolder,
    trackerLogSink: TrackerLogSink
) : ViewModel() {
    private val latestMessage = MutableStateFlow("Tracker readings are not available yet.")
    private val liveTrackerState: Flow<TrackerSnapshot?> = runtimeHolder.engine.flatMapLatest { engine ->
        engine?.observeState()?.map<TrackerSnapshot, TrackerSnapshot?> { it } ?: flowOf(null)
    }
    private val history: Flow<DashboardHistory?> = trackerLogSink.observeRecent(2_000).map { records ->
        val startOfTodayUtc = utcDayStart(System.currentTimeMillis())
        val today = records.filter { it.t >= startOfTodayUtc && it.t < startOfTodayUtc + MILLIS_PER_UTC_DAY }
        if (today.isEmpty()) null else DashboardHistory(
            sampleCount = today.size,
            sunFoundCount = today.count { it.sunFound },
            meanJitterPx = today.mapNotNull { it.jitter?.toDouble() }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        )
    }

    init { refreshAiSummary() }

    val uiState: StateFlow<HomeUiState> = combine(
        roboDevice.observeSnapshot(), latestMessage, liveTrackerState, history
    ) { robo: RoboSnapshot, assistantMessage: String, tracker: TrackerSnapshot?, today: DashboardHistory? ->
        val connected = tracker?.link == LinkState.CONNECTED || robo.connected
        val hasFault = tracker?.faults?.isNotEmpty() == true || robo.mode == RoboMode.FAULT
        HomeUiState(
            isLoading = false,
            isConnected = connected,
            connectionText = if (connected) "ESP32 Wi-Fi link connected" else "Offline · no live tracker state",
            mode = if (hasFault) RoboMode.FAULT else robo.mode,
            // The controller has no encoder; this is a camera-derived estimate, never a measured angle.
            panelAngleDeg = tracker?.estimatedPanelAngleDeg,
            targetAngleDeg = tracker?.targetAngleDeg,
            sunAzimuthDeg = tracker?.sunAzimuthDeg,
            sunElevationDeg = tracker?.sunElevationDeg,
            isCalibrated = tracker?.calibrated,
            // These values require electrical sensors not present in the current hardware contract.
            generationWatts = null,
            batteryPercent = null,
            environmentText = "Not measured",
            todayTrackingCycles = today?.sampleCount,
            todaySunFoundCount = today?.sunFoundCount,
            todayMeanJitterPx = today?.meanJitterPx,
            safetyLevel = if (hasFault) SafetyLevel.FAULT else SafetyLevel.NORMAL,
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

    private data class DashboardHistory(
        val sampleCount: Int,
        val sunFoundCount: Int,
        val meanJitterPx: Float?
    )

    private companion object {
        const val MILLIS_PER_UTC_DAY = 86_400_000L
        fun utcDayStart(now: Long): Long = Math.floorDiv(now, MILLIS_PER_UTC_DAY) * MILLIS_PER_UTC_DAY
    }
}
