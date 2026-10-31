package com.solarrobo.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solarrobo.app.weather.WeatherUiState
import com.solarrobo.core.contracts.AccessMode
import com.solarrobo.core.ui.components.MetricTile
import com.solarrobo.core.ui.components.SolarPanelHero
import com.solarrobo.core.ui.theme.LocalAccessMode
import com.solarrobo.core.ui.theme.SolarRoboTheme
import com.solarrobo.core.weather.AdvisoryAction

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    weatherViewModel: WeatherViewModel,
    onNavigateToSimulator: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val weather by weatherViewModel.uiState.collectAsStateWithLifecycle()
    HomeScreenContent(
        state = state,
        weather = weather,
        onNavigateToSimulator = onNavigateToSimulator,
        onNavigateToCamera = onNavigateToCamera,
        onNavigateToOnboarding = onNavigateToOnboarding,
        onNavigateToRoute = onNavigateToRoute,
        onRefreshAi = viewModel::refreshAiSummary,
        onRefreshWeather = weatherViewModel::refresh,
        modifier = modifier
    )
}

@Composable
fun HomeScreenContent(
    state: HomeUiState,
    weather: WeatherUiState,
    onNavigateToSimulator: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onRefreshAi: () -> Unit,
    onRefreshWeather: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mode = LocalAccessMode.current
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("ASTRA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
            }
            Text(if (mode == AccessMode.OPERATOR) "Operator" else "Viewer", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SolarPanelHero(
            panelAngleDeg = state.panelAngleDeg,
            status = if (state.isConnected) "Tracking active · local controller connected" else "Waiting for local controller link"
        )
        if (state.isLoading) {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Loading local tracker state…", modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            ConnectionCard(state, onNavigateToOnboarding)
            if (!state.isConnected && state.panelAngleDeg == null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No live tracker data yet", style = MaterialTheme.typography.titleMedium)
                        Text("Connect the ESP32 access point, set the tracker location, then calibrate. Unmeasured electrical and weather values stay marked as Not measured.", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = onNavigateToOnboarding) { Text("Set up tracker") }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                    label = "Panel angle",
                    value = state.panelAngleDeg?.let { "%.1f°".format(it) } ?: "Not measured",
                    source = if (state.panelAngleDeg == null) "Not measured" else "Measured",
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Solar target",
                    value = state.targetAngleDeg?.let { "%.1f°".format(it) } ?: "Not measured",
                    source = "Calculated",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(label = "Power", value = "Not measured", source = "Not measured", modifier = Modifier.weight(1f))
                MetricTile(label = "Battery", value = "Not measured", source = "Not measured", modifier = Modifier.weight(1f))
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tracker status", style = MaterialTheme.typography.titleMedium)
                    Text("Link: ${if (state.isConnected) "Connected" else "Offline"}")
                    Text("Calibration: ${when (state.isCalibrated) { true -> "Complete"; false -> "Required"; null -> "Not available" }}")
                    Text("Sun azimuth: ${state.sunAzimuthDeg?.let { "%.1f° · calculated".format(it) } ?: "Not available"}")
                    Text("Sun elevation: ${state.sunElevationDeg?.let { "%.1f° · calculated".format(it) } ?: "Not available"}")
                    Text("Environment: ${state.environmentText}")
                }
            }
            HistoryCard(state)
        }
        WeatherAdvisoryCard(weather, onRefreshWeather)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Offline guidance", style = MaterialTheme.typography.titleMedium)
                Text(state.latestMessage)
                OutlinedButton(onClick = onRefreshAi) { Text("Refresh guidance") }
                OutlinedButton(onClick = { onNavigateToRoute("talk") }) { Text("Talk to ASTRA") }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNavigateToCamera, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.height(0.dp)); Text(" Camera")
            }
            OutlinedButton(onClick = { onNavigateToRoute("settings") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Text(" Settings")
            }
        }
        OutlinedButton(onClick = { onNavigateToRoute("track") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.TrackChanges, contentDescription = null)
            Text(" Open tracker controls")
        }
        OutlinedButton(onClick = onNavigateToSimulator, modifier = Modifier.fillMaxWidth()) { Text("Open simulator · simulated values") }
        Text("ASTRA 2.1.0-astra-ui · values are local, calculated, camera-estimated, or explicitly Not measured.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ConnectionCard(state: HomeUiState, onSetup: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = if (state.isConnected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (state.isConnected) "Controller connected" else "Controller not connected", style = MaterialTheme.typography.titleLarge)
            Text(state.connectionText)
            Text("Single-axis tracking · local Wi-Fi · sensorless position estimate", style = MaterialTheme.typography.bodySmall)
            if (!state.isConnected) Button(onClick = onSetup) { Text("Set up tracker") }
        }
    }
}

@Composable
private fun DashboardMetricCard(title: String, value: String, note: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(note, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HistoryCard(state: HomeUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Recent local tracking history", style = MaterialTheme.typography.titleMedium)
            val count = state.todayTrackingCycles
            if (count == null || count == 0) {
                Text("No tracking samples found in the most recent 2,000 log rows for today (UTC).")
            } else {
                Text("Samples in most recent 2,000 rows today (UTC): $count")
                Text("Sun detected: ${state.todaySunFoundCount ?: 0} of these samples · camera classification")
                Text("Mean image jitter: ${state.todayMeanJitterPx?.let { "%.2f px · image statistic".format(it) } ?: "Not available"}")
            }
        }
    }
}

@Composable
private fun WeatherAdvisoryCard(weather: WeatherUiState, onRefresh: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Weather intelligence · advisory only", style = MaterialTheme.typography.titleMedium)
            Text("Camera: ${weather.cameraStatus} · Model: ${weather.modelStatus}")
            val prediction = weather.prediction
            Text("Sky class: ${prediction?.label?.replace('_', ' ') ?: "Not available"}")
            Text("CNN confidence: ${prediction?.let { "%.1f%%".format(it.confidence * 100) + if (it.accepted) " · accepted" else " · low confidence" } ?: "Not available"}")
            val visualRisk = when {
                prediction == null || !prediction.accepted -> "Unknown · low confidence or no image"
                prediction.label == "cumulonimbus_or_storm_like" -> "Storm-like visual indicator · not a verified storm"
                prediction.label in listOf("towering_cumulus", "fog_or_obscured_sky", "rain_or_precipitation", "overcast_or_stratus") -> "Potential visual weather concern · not a safety measurement"
                else -> "No storm-like class detected · not a safety clearance"
            }
            Text("Visual advisory: $visualRisk")
            Text("Apparent sky motion: ${weather.opticalFlow?.let { "dx %+.1f, dy %+.1f px/s".format(it.dxPixelsPerSecond ?: 0.0, it.dyPixelsPerSecond ?: 0.0) } ?: "Not available"} · image drift, not wind speed")
            Text("Forecast: ${weather.forecastStatusText}")
            Text("Forecast location: ${weather.locationText}")
            Text("Combined advisory: ${weather.recommendation.risk} · ${when (weather.recommendation.action) {
                AdvisoryAction.CONSIDER_PROTECTIVE_STOW -> "Review manual protective-stow conditions"
                AdvisoryAction.REVIEW -> "Review conditions"
                AdvisoryAction.CONTINUE_TRACKING -> "No elevated advisory"
            }}")
            weather.recommendation.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            Text("Climatology: ${weather.climatologyText}")
            Text("Verified local outcomes: ${weather.learning.verifiedCount}/${weather.learning.requiredMinimum}; learning ${if (weather.learning.sufficient) "eligible" else "insufficient"}.")
            weather.error?.let { Text("Status: $it", color = MaterialTheme.colorScheme.error) }
            Button(onClick = onRefresh, enabled = !weather.isRefreshing) { Text(if (weather.isRefreshing) "Refreshing…" else "Refresh forecast") }
            Text("Open-Meteo attribution · CC BY 4.0. Retrieval time is used as a proxy because issue time is unavailable. CNN and pixel statistics are local image estimates, not measured rain, wind, irradiance, or verified storm probabilities. Weather intelligence never moves motors.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true, name = "Dashboard · empty · light")
@Composable
private fun HomeScreenEmptyPreview() {
    SolarRoboTheme {
        HomeScreenContent(HomeUiState(), WeatherUiState(), {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Dashboard · populated · light")
@Composable
private fun HomeScreenPopulatedPreview() {
    SolarRoboTheme {
        HomeScreenContent(
            HomeUiState(isConnected = true, panelAngleDeg = 22.5f, targetAngleDeg = 24f, sunAzimuthDeg = 190f, sunElevationDeg = 38f, isCalibrated = true, todayTrackingCycles = 42, todaySunFoundCount = 34, todayMeanJitterPx = 1.7f),
            WeatherUiState(), {}, {}, {}, {}, {}, {}
        )
    }
}
