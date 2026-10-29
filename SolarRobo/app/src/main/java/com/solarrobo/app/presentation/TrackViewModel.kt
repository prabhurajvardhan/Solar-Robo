package com.solarrobo.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.app.di.TrackerRuntimeHolder
import com.solarrobo.core.camera.CameraXFrameSource
import com.solarrobo.core.contracts.CalibrationStep
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.LinkState
import com.solarrobo.core.contracts.SafetyContext
import com.solarrobo.core.contracts.SafetyDecision
import com.solarrobo.core.contracts.SafetyGate
import com.solarrobo.core.contracts.TrackerSnapshot
import com.solarrobo.core.device.RoboDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrackUiState(
    val snapshot: RoboSnapshot? = null,
    val tracker: TrackerSnapshot? = null,
    val cameraExposureReport: String = "Camera exposure controls not checked",
    val calibrationStep: CalibrationStep? = null,
    val emergencyLatched: Boolean = false,
    val message: String? = null,
    val isSendingStop: Boolean = false
)

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val roboDevice: RoboDevice,
    private val safetyGate: SafetyGate,
    private val runtimeHolder: TrackerRuntimeHolder,
    private val cameraSource: CameraXFrameSource
) : ViewModel() {
    private val controls = MutableStateFlow(TrackUiState())
    val uiState: StateFlow<TrackUiState> = combine(roboDevice.observeSnapshot(), controls) { snapshot, state ->
        state.copy(snapshot = snapshot, emergencyLatched = safetyGate.isEmergencyLatched())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackUiState())

    init {
        viewModelScope.launch {
            runtimeHolder.engine.flatMapLatest { engine -> engine?.observeState() ?: flowOf(null) }
                .collect { tracker -> controls.update { it.copy(tracker = tracker) } }
        }
        viewModelScope.launch {
            runtimeHolder.error.collect { message -> if (message != null) controls.update { it.copy(message = message) } }
        }
        viewModelScope.launch {
            cameraSource.exposureControlReport.collect { report -> controls.update { it.copy(cameraExposureReport = report) } }
        }
    }

    fun emergencyStop() {
        safetyGate.triggerEmergencyStop()
        controls.update { it.copy(emergencyLatched = true, isSendingStop = true, message = null) }
        viewModelScope.launch {
            runCatching { runtimeHolder.engine.value?.stop() }
            val decision = safetyGate.evaluate(RoboCommand.StopMotion, SafetyContext(LinkState.OFFLINE, false, true, false, false, false))
            val result = if (decision == SafetyDecision.Allow) roboDevice.sendCommand(RoboCommand.StopMotion) else null
            controls.update { it.copy(
                emergencyLatched = true,
                isSendingStop = false,
                message = when {
                    result?.accepted == true && safetyGate.isLatchPersisted() -> "Stop command sent to controller and E-stop latch persisted."
                    result?.accepted == true -> "Stop sent, but the safety latch could not be persisted. Keep motor-driver power disconnected until checked."
                    else -> "Stop could not be confirmed: ${result?.reason ?: (decision as? SafetyDecision.Block)?.reason ?: "tracker offline"}. Disconnect motor-driver power locally."
                }
            ) }
        }
    }

    fun resume() {
        safetyGate.resumeAfterEmergencyStop()
        controls.update { it.copy(emergencyLatched = false, message = "Safety latch cleared. A calibrated tracker may resume on its next cycle only if every link, limit, and fault interlock passes.") }
        runtimeHolder.engine.value?.let { engine ->
            viewModelScope.launch {
                runCatching { engine.start() }.onFailure { error ->
                    controls.update { it.copy(message = "Resume was not accepted: ${error.localizedMessage ?: "verify link and faults"}") }
                }
            }
        }
    }

    fun setHome() = withEngine("Confirm that the panel is physically flat before setting home.") { it.setHome() }

    fun calibrate() {
        val engine = runtimeHolder.engine.value
        if (engine == null) {
            controls.update { it.copy(message = "Start the tracker service and connect the ESP32 before calibration.") }
            return
        }
        viewModelScope.launch {
            engine.calibrate().collect { step -> controls.update { it.copy(calibrationStep = step, message = step.message) } }
        }
    }

    fun resetFault() = withEngine("Start the tracker before resetting faults.") { it.resetFault() }

    private fun withEngine(unavailableMessage: String, action: suspend (com.solarrobo.core.contracts.TrackerEngine) -> Unit) {
        val engine = runtimeHolder.engine.value
        if (engine == null) controls.update { it.copy(message = unavailableMessage) }
        else viewModelScope.launch {
            runCatching { action(engine) }.onFailure { error -> controls.update { it.copy(message = error.localizedMessage ?: "Tracker action failed") } }
        }
    }
}
