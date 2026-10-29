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
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.solarrobo.core.ui.theme.SolarAmber
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
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Solar Robo", style = MaterialTheme.typography.headlineMedium, color = SolarAmber)
        Text("Sensorless single-axis tracker", style = MaterialTheme.typography.titleMedium)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (state.isConnected) "Tracker connected" else "Tracker not connected", style = MaterialTheme.typography.titleLarge)
                Text(state.connectionText, style = MaterialTheme.typography.bodyMedium)
                Text("Connect to the ESP32 Wi-Fi and complete setup before enabling tracking.")
                Button(onClick = onNavigateToOnboarding) { Text("Set up tracker") }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.WbSunny, contentDescription = null)
                    Text("Tracking overview", style = MaterialTheme.typography.titleMedium)
                }
                Text("Panel angle: ${state.panelAngleDeg?.let { "%.1f° (estimated)".format(it) } ?: "Not measured"}")
                Text("Power output: ${state.generationWatts?.let { "%.0f W (estimate)".format(it) } ?: "Not measured"}")
                Text("Battery: ${state.batteryPercent?.let { "%.0f%%".format(it) } ?: "Not measured"}")
                Text("Environment: ${state.environmentText}")
                Text("No electrical or physical weather sensors are configured. Camera observations are estimates—not measured weather or power.", style = MaterialTheme.typography.bodySmall)
            }
        }
        WeatherAdvisoryCard(weather, onRefreshWeather)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Offline assistant", style = MaterialTheme.typography.titleMedium)
                Text(state.latestMessage)
                OutlinedButton(onClick = onRefreshAi) { Text("Refresh guidance") }
                OutlinedButton(onClick = { onNavigateToRoute("talk") }) { Text("Talk to Robo") }
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
        OutlinedButton(onClick = onNavigateToSimulator, modifier = Modifier.fillMaxWidth()) { Text("Open simulator (simulated values)") }
    }
}

@Composable
private fun WeatherAdvisoryCard(weather: WeatherUiState, onRefresh: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Weather intelligence · advisory only", style = MaterialTheme.typography.titleMedium)
            Text("Model: ${weather.modelStatus}")
            Text("Camera: ${weather.cameraStatus}")
            val prediction = weather.prediction
            Text("Sky class: ${prediction?.label?.replace('_', ' ') ?: "Not available"}")
            Text("CNN confidence: ${prediction?.let { "%.1f%%".format(it.confidence * 100) + if (it.accepted) " · accepted" else " · LOW CONFIDENCE" } ?: "Not available"}")
            val visualRisk = when {
                prediction == null || !prediction.accepted -> "Unknown · low confidence or no image"
                prediction.label == "cumulonimbus_or_storm_like" -> "Storm-like visual indicator · %.0f%% confidence; not a verified storm".format(prediction.confidence * 100)
                prediction.label in listOf("towering_cumulus", "fog_or_obscured_sky", "rain_or_precipitation", "overcast_or_stratus") -> "Potential weather concern · %.0f%% model confidence".format(prediction.confidence * 100)
                else -> "No storm-like class detected · not a safety clearance"
            }
            Text("Visual risk: $visualRisk")
            Text("Short-term obstruction: ${weather.obstructionEstimate}")
            val flow = weather.opticalFlow
            val motionText = if (flow?.dxPixelsPerSecond != null && flow.dyPixelsPerSecond != null) {
                "dx %+.1f, dy %+.1f px/s · %d patches · %.0f%% match confidence".format(
                    flow.dxPixelsPerSecond, flow.dyPixelsPerSecond, flow.matchedPatches, (flow.confidence ?: 0.0) * 100
                )
            } else "Not available"
            Text("Apparent sky motion: $motionText. Camera image drift only; not wind speed.")
            Text("Sky brightness: ${weather.brightness?.let { "%.1f/255 · image heuristic".format(it) } ?: "Not available"}; white fraction: ${weather.whiteFraction?.let { "%.2f · image statistic".format(it) } ?: "Not available"}")
            Text("Forecast: ${weather.forecastStatusText}")
            Text("Forecast location: ${weather.locationText}")
            Text("Combined advisory: ${weather.recommendation.risk} · ${when (weather.recommendation.action) {
                AdvisoryAction.CONSIDER_PROTECTIVE_STOW -> "Consider manual protective-stow review"
                AdvisoryAction.REVIEW -> "Review conditions"
                AdvisoryAction.CONTINUE_TRACKING -> "No elevated advisory"
            }}")
            weather.recommendation.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            Text("Climatology: ${weather.climatologyText}")
            Text("Verified local outcomes: ${weather.learning.verifiedCount}/${weather.learning.requiredMinimum}; local learning is ${if (weather.learning.sufficient) "eligible" else "insufficient"}.")
            weather.error?.let { Text("Status: $it", color = MaterialTheme.colorScheme.error) }
            Button(onClick = onRefresh, enabled = !weather.isRefreshing) { Text(if (weather.isRefreshing) "Refreshing…" else "Refresh forecast") }
            Text("Open-Meteo forecast attribution · CC BY 4.0. Its issue time is unavailable; retrieval time is a proxy. CNN output and pixel statistics are local image estimates, not measured rain, wind, irradiance, or verified storm probabilities. Weather intelligence never moves motors. Verify hardware position, limits, and stow direction.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SolarRoboTheme {
        HomeScreenContent(
            state = HomeUiState(), weather = WeatherUiState(),
            onNavigateToSimulator = {}, onNavigateToCamera = {}, onNavigateToOnboarding = {},
            onNavigateToRoute = {}, onRefreshAi = {}, onRefreshWeather = {}
        )
    }
}
