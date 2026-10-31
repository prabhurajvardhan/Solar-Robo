package com.solarrobo.app.presentation

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solarrobo.app.weather.WeatherUiState
import com.solarrobo.core.ui.components.AlertBanner
import com.solarrobo.core.ui.components.AstraCard
import com.solarrobo.core.ui.components.MetricTile
import com.solarrobo.core.ui.components.SectionHeader
import com.solarrobo.core.ui.components.SourceTag
import com.solarrobo.core.ui.theme.AstraMint
import com.solarrobo.core.ui.theme.AstraPrimary
import com.solarrobo.core.ui.theme.SunAmber
import com.solarrobo.core.weather.ForecastStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    weatherViewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val weather by weatherViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var predictions by remember { mutableStateOf(false) }
    val days = state.daily.take(30).reversed()
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { predictions = false }, modifier = Modifier.weight(1f)) { Text("Sky") }
            Button(onClick = { predictions = true }, modifier = Modifier.weight(1f)) { Text("Predictions") }
        }
        AstraCard {
            SectionHeader(if (predictions) "Prediction intelligence" else "Sky conditions", weather.locationText)
            Text(if (predictions) weather.modelStatus else weather.cameraStatus, style = MaterialTheme.typography.bodyMedium)
            SourceTag(if (predictions) "On-device advisory" else "Camera estimate")
            if (!predictions) {
                Text("${weather.prediction?.label?.replace('_', ' ') ?: "Not measured"}", style = MaterialTheme.typography.headlineMedium)
                Text("Confidence ${weather.prediction?.let { "%.0f%%".format(it.confidence * 100) } ?: "Not measured"}")
            } else {
                Text("${weather.learning.verifiedCount} / ${weather.learning.requiredMinimum} verified outcomes", style = MaterialTheme.typography.headlineMedium)
                Text(if (weather.learning.sufficient) "Learning baseline ready" else "Learning, needs more days of data")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("Cloud cover", weather.forecast?.points?.firstOrNull()?.cloudCoverPercent?.let { "%.0f%%".format(it) } ?: "Not measured", "Forecast", Modifier.weight(1f))
            MetricTile("Wind", weather.forecast?.points?.firstOrNull()?.windSpeedMps?.let { "%.1f m/s".format(it) } ?: "Not measured", "Forecast", Modifier.weight(1f))
        }
        AstraCard {
            SectionHeader(if (predictions) "Cloud prediction" else "Next hours", weather.forecastStatusText)
            val points = weather.forecast?.points.orEmpty().take(12)
            if (points.isEmpty()) {
                Text("No forecast points available. Set installation coordinates in Settings or refresh when online.")
            } else {
                ForecastChart(points.mapNotNull { it.cloudCoverPercent }, "Cloud cover forecast")
                points.take(5).forEach { point ->
                    Text("${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(point.validAt.toEpochMilli()))} · cloud ${point.cloudCoverPercent?.let { "%.0f%%".format(it) } ?: "Not measured"} · precipitation ${point.precipitationMm?.let { "%.1f mm".format(it) } ?: "Not measured"}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        AlertBanner("${weather.recommendation.risk}: ${weather.recommendation.reasons.firstOrNull() ?: "Weather advisory unavailable"}", danger = weather.recommendation.risk.name == "HIGH")
        AstraCard {
            SectionHeader("Local tracker history", "Latest 2,000 cycles")
            if (state.recent.isEmpty() && days.isEmpty()) {
                Text("No tracker history yet. Start the foreground tracker to record local observations.")
            } else {
                Text("Detailed cycles retained: ${state.recent.size}")
                Text("Sun-found rate: ${state.recent.takeIf { it.isNotEmpty() }?.let { "%.0f%%".format(it.count { row -> row.sunFound } * 100f / it.size) } ?: "Not available"}")
                Text("Wind-stow cycles: ${state.recent.count { it.windHold }} · jam events: ${state.recent.count { it.jam }}")
                Button(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_SUBJECT, "Solar Robo tracker history")
                        putExtra(Intent.EXTRA_TEXT, viewModel.latestCsv())
                    }
                    context.startActivity(Intent.createChooser(share, "Export latest tracker cycles"))
                }) { Text("Export latest 2,000 cycles as CSV") }
            }
        }
        AstraCard {
            SectionHeader("Daily history · last 30 available days")
            if (days.isEmpty()) Text("Learning, needs more days of data") else {
                val maxSamples = days.maxOfOrNull { it.sampleCount }?.coerceAtLeast(1) ?: 1
                val description = days.joinToString { "${dayLabel(it.dayStartMs)}: ${it.sampleCount} cycles" }
                Canvas(Modifier.fillMaxWidth().height(128.dp).semantics { contentDescription = "Daily logged tracker cycles. $description" }) {
                    val gap = 3.dp.toPx()
                    val barWidth = ((size.width - gap * (days.size - 1)) / days.size).coerceAtLeast(1f)
                    days.forEachIndexed { index, day ->
                        val barHeight = size.height * day.sampleCount / maxSamples.toFloat()
                        drawRoundRect(SunAmber, Offset(index * (barWidth + gap), size.height - barHeight), androidx.compose.ui.geometry.Size(barWidth, barHeight), CornerRadius(4.dp.toPx(), 4.dp.toPx()))
                    }
                }
            }
            Text("Detailed rows are retained for 90 days; older cycles are rolled into daily summaries.", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = weatherViewModel::refresh, enabled = !weather.isRefreshing, modifier = Modifier.fillMaxWidth()) { Text(if (weather.isRefreshing) "Refreshing…" else "Refresh forecast") }
    }
}

@Composable
private fun ForecastChart(values: List<Double>, description: String) {
    Canvas(Modifier.fillMaxWidth().height(100.dp).semantics { contentDescription = description }) {
        if (values.size < 2) return@Canvas
        val max = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val step = size.width / (values.size - 1)
        values.zipWithNext().forEachIndexed { index, pair ->
            val start = Offset(index * step, size.height - (pair.first / max * size.height).toFloat())
            val end = Offset((index + 1) * step, size.height - (pair.second / max * size.height).toFloat())
            drawLine(AstraPrimary, start, end, strokeWidth = 5.dp.toPx())
            drawCircle(AstraPrimary, 4.dp.toPx(), end)
        }
    }
}

private fun dayLabel(timestamp: Long): String = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
