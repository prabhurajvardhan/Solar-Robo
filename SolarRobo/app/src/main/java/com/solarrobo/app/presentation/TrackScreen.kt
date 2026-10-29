package com.solarrobo.app.presentation

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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solarrobo.core.contracts.AccessMode
import com.solarrobo.core.ui.theme.LocalAccessMode
import com.solarrobo.core.ui.components.ViewerAccessTooltip
import com.solarrobo.app.service.TrackerForegroundService
import com.solarrobo.core.ui.theme.SolarRoboTheme

@Composable
fun TrackScreen(viewModel: TrackViewModel, modifier: Modifier = Modifier, onSetup: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOperator = LocalAccessMode.current == AccessMode.OPERATOR
    val context = LocalContext.current
    val connected = state.snapshot?.connected == true
    val active = state.tracker != null
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
            Text("Manual angle · +/-70° (disabled until a verified control path is available)")
            Slider(value = 0f, onValueChange = {}, valueRange = -70f..70f, enabled = false)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ViewerAccessTooltip(isOperator, modifier = Modifier.weight(1f)) { actionModifier ->
                    OutlinedButton(onClick = onSetup, enabled = isOperator, modifier = actionModifier.fillMaxWidth()) { Text("Set up tracker") }
                }
            }
            Text("Battery, electrical output, physical wind, rain, and panel encoders are not measured by this hardware.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackScreenPreview() { SolarRoboTheme { Text("Track preview · offline · E-stop visible") } }
