package com.solarrobo.app.presentation

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.solarrobo.core.contracts.AccessMode
import com.solarrobo.core.contracts.LightFollowPhase
import com.solarrobo.core.ui.theme.LocalAccessMode
import com.solarrobo.core.ui.components.ViewerAccessTooltip
import com.solarrobo.app.service.TrackerForegroundService
import com.solarrobo.core.ui.theme.SolarRoboTheme

@Composable
fun TrackScreen(viewModel: TrackViewModel, modifier: Modifier = Modifier, onSetup: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOperator = LocalAccessMode.current == AccessMode.OPERATOR
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) { PreviewView(context) }
    val connected = state.snapshot?.connected == true
    val active = state.tracker != null
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (active) viewModel.bindLightFollowPreview(previewView)
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    viewModel.stopLightFollow()
                    viewModel.clearLightFollowPreview()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopLightFollow()
        }
    }
    Scaffold { insets ->
        Column(
            modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Track", style = MaterialTheme.typography.headlineMedium)
            if (!isOperator) Text("Viewer mode · operator controls are disabled. Unlock with your local PIN to make changes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(colors = CardDefaults.cardColors(containerColor = if (connected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (connected) "ESP32 connected" else "Tracker offline", style = MaterialTheme.typography.titleMedium)
                    Text(if (connected) "Local Wi-Fi link verified. Tracking remains gated by calibration and safety checks." else "No controller link is active. A stop request may not reach the motor while offline.")
                }
            }
            ViewerAccessTooltip(isOperator || active) { actionModifier ->
                Button(
                    onClick = { if (active) TrackerForegroundService.stop(context) else TrackerForegroundService.start(context) },
                    modifier = actionModifier.fillMaxWidth().heightIn(min = 52.dp),
                    enabled = active || (connected && isOperator)
                ) { Text(if (active) "Stop tracking service" else "Start camera tracking") }
            }
            state.message?.let { Text(it, color = if (state.calibrationStep?.complete == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
            val tracker = state.tracker
            Text("Panel angle: ${tracker?.estimatedPanelAngleDeg?.let { "%.1f° (estimate)".format(it) } ?: "Not available"}")
            Text("Single-axis target: ${tracker?.targetAngleDeg?.let { "%.1f° (calculated)".format(it) } ?: "Not available"}")
            Text("Angle source: ${tracker?.angleSource?.name?.replace('_', ' ') ?: "No tracker service"}")
            Text(state.cameraExposureReport, style = MaterialTheme.typography.bodySmall)
            Text("Sun azimuth: ${tracker?.sunAzimuthDeg?.let { "%.1f° (calculated)".format(it) } ?: "Not available"}")
            Text("Sun elevation: ${tracker?.sunElevationDeg?.let { "%.1f° (calculated)".format(it) } ?: "Not available"}")
            tracker?.faults?.takeIf { it.isNotEmpty() }?.let { Text("Active faults: ${it.joinToString()}", color = MaterialTheme.colorScheme.error) }
            state.calibrationStep?.let { step ->
                Text("Calibration · ${step.name} · ${(step.progress * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
                Text(step.message, style = MaterialTheme.typography.bodySmall)
            }
            ViewerAccessTooltip(isOperator) { actionModifier ->
                OutlinedButton(
                    onClick = viewModel::setHome,
                    enabled = isOperator && active && !state.emergencyLatched,
                    modifier = actionModifier.fillMaxWidth()
                ) { Text("I have physically flattened the panel — Set home") }
            }
            Text("Only confirm Home after manually placing the panel flat; firmware does not move it.", style = MaterialTheme.typography.bodySmall)
            ViewerAccessTooltip(isOperator) { actionModifier ->
                Button(
                    onClick = viewModel::calibrate,
                    enabled = isOperator && active && connected && !state.emergencyLatched,
                    modifier = actionModifier.fillMaxWidth()
                ) { Text(if (state.calibrationStep?.complete == true) "Calibrate again" else "Run camera calibration") }
            }
            Text("Calibration observes filtered sun images and drift for about six minutes. Never aim an unfiltered camera at the sun.", style = MaterialTheme.typography.bodySmall)
            Text("Camera frames are processed locally for sun/sky statistics; the app does not save or upload them.", style = MaterialTheme.typography.bodySmall)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Light-follow demo", style = MaterialTheme.typography.titleMedium)
                    Text("Follows a bright, small camera spot using deterministic pixel feedback. This is a torch/demo mode, not solar tracking.", style = MaterialTheme.typography.bodySmall)
                    LaunchedEffect(previewView, active) { if (active) viewModel.bindLightFollowPreview(previewView) }
                    DisposableEffect(previewView) {
                        onDispose { viewModel.clearLightFollowPreview() }
                    }
                    Box(Modifier.fillMaxWidth().height(200.dp)) {
                        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                        val crosshairColor = MaterialTheme.colorScheme.onPrimaryContainer
                        val markerColor = MaterialTheme.colorScheme.error
                        Canvas(Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            drawLine(crosshairColor, Offset(center.x - 14.dp.toPx(), center.y), Offset(center.x + 14.dp.toPx(), center.y), strokeWidth = 2.dp.toPx())
                            drawLine(crosshairColor, Offset(center.x, center.y - 14.dp.toPx()), Offset(center.x, center.y + 14.dp.toPx()), strokeWidth = 2.dp.toPx())
                            val lx = state.lightFollow.lightX
                            val ly = state.lightFollow.lightY
                            if (lx != null && ly != null) {
                                val sx = lx / 320f * size.width
                                val sy = ly / 240f * size.height
                                drawCircle(markerColor, radius = 10.dp.toPx(), center = Offset(sx, sy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                            }
                        }
                    }
                    Text("State: ${state.lightFollow.phase.displayName()}", style = MaterialTheme.typography.titleLarge)
                    Text("Spot: ${state.lightFollow.lightX?.let { "x %.1f px".format(it) } ?: "Not detected"} · error: ${state.lightFollow.errorPx?.let { "%.1f px".format(it) } ?: "Not measured"} · ${state.lightFollow.errorDeg?.let { "%.2f°".format(it) } ?: "Not measured"}")
                    Text("Estimated panel angle: ${tracker?.estimatedPanelAngleDeg?.let { "%.1f°".format(it) } ?: "Not available"} · moves: ${state.lightFollow.moves}")
                    state.lightFollow.darkFilterWarning.let { warning ->
                        if (warning || (tracker?.sunElevationDeg ?: 0f) > 8f) {
                            Text("Dark-filter warning: never use this mode with the real sun in view. Cover the camera with a proper solar filter or point it away from the sun.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    state.lightFollow.message?.let { Text(it, color = if (state.lightFollow.phase == LightFollowPhase.ERROR || state.lightFollow.phase == LightFollowPhase.END_OF_TRAVEL) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("Keep hands and cables clear of the panel. Follow mode sends motor commands only after a deliberate Start and stops on error, link loss, lock, E-stop, or app lifecycle shutdown.", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = viewModel::startLightFollow,
                        enabled = isOperator && active && connected && tracker?.calibrated == true && tracker.faults.isEmpty() && !state.emergencyLatched && state.lightFollow.phase in setOf(LightFollowPhase.IDLE, LightFollowPhase.ERROR, LightFollowPhase.LOST, LightFollowPhase.END_OF_TRAVEL),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    ) { Text("Start light-follow") }
                    Button(
                        onClick = viewModel::stopLightFollow,
                        enabled = state.lightFollow.phase !in setOf(LightFollowPhase.IDLE, LightFollowPhase.ERROR, LightFollowPhase.END_OF_TRAVEL),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("STOP LIGHT-FOLLOW") }
                }
            }
            Button(
                onClick = viewModel::emergencyStop,
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(if (state.isSendingStop) "SENDING STOP…" else "E-STOP · STOP MOTION") }
            if (state.emergencyLatched) {
                ViewerAccessTooltip(isOperator) { actionModifier ->
                    OutlinedButton(onClick = viewModel::resume, enabled = isOperator, modifier = actionModifier.fillMaxWidth()) { Text("Resume (clear safety latch)") }
                }
            }
            ViewerAccessTooltip(isOperator) { actionModifier ->
                OutlinedButton(onClick = viewModel::resetFault, enabled = isOperator && active, modifier = actionModifier.fillMaxWidth()) { Text("Reset recoverable fault") }
            }
            Text("Manual jog · 1° steps", style = MaterialTheme.typography.titleMedium)
            Text("Jog pauses automatic tracking. Stop and restart the tracker service after the adjustment. Travel limits, calibration, link, fault and E-stop interlocks remain active.", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val jogEnabled = isOperator && active && connected && tracker?.calibrated == true && tracker.faults.isEmpty() && !state.emergencyLatched
                ViewerAccessTooltip(isOperator) { actionModifier ->
                    OutlinedButton(onClick = { viewModel.jog(-1f) }, enabled = jogEnabled, modifier = actionModifier.weight(1f).heightIn(min = 48.dp)) { Text("−1° West") }
                }
                ViewerAccessTooltip(isOperator) { actionModifier ->
                    OutlinedButton(onClick = { viewModel.jog(1f) }, enabled = jogEnabled, modifier = actionModifier.weight(1f).heightIn(min = 48.dp)) { Text("+1° East") }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ViewerAccessTooltip(isOperator, modifier = Modifier.weight(1f)) { actionModifier ->
                    OutlinedButton(onClick = onSetup, enabled = isOperator, modifier = actionModifier.fillMaxWidth()) { Text("Set up tracker") }
                }
            }
            Text("Battery, electrical output, physical wind, rain, and panel encoders are not measured by this hardware.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun LightFollowPhase.displayName(): String = when (this) {
    LightFollowPhase.IDLE -> "Idle"
    LightFollowPhase.SEARCHING -> "Searching"
    LightFollowPhase.LEARNING -> "Learning"
    LightFollowPhase.LOCKED -> "Locked"
    LightFollowPhase.MOVING -> "Moving"
    LightFollowPhase.LOST -> "Lost · holding position"
    LightFollowPhase.END_OF_TRAVEL -> "End of travel"
    LightFollowPhase.ERROR -> "Stopped · error"
}

@Preview(showBackground = true)
@Composable
private fun TrackScreenPreview() { SolarRoboTheme { Text("Track preview · offline · E-stop visible") } }
