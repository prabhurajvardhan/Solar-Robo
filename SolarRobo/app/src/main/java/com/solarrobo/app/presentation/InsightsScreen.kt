package com.solarrobo.app.presentation

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InsightsScreen(viewModel: InsightsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val days = state.daily.take(30).reversed()
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Insights", style = MaterialTheme.typography.headlineMedium)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Local tracker history", style = MaterialTheme.typography.titleMedium)
                if (state.recent.isEmpty() && days.isEmpty()) {
                    Text("No tracker history yet. Start the foreground tracker to record local observations.")
                    Text("Charts appear after recorded cycles. No forecast is available; camera descriptors are heuristics, not physical weather measurements.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Detailed cycles retained: ${state.recent.size} (latest 2,000 shown)")
                    Text("Sun-found rate: ${state.recent.takeIf { it.isNotEmpty() }?.let { "%.0f%%".format(it.count { row -> row.sunFound } * 100f / it.size) } ?: "Not available"}")
                    Text("Wind-stow cycles: ${state.recent.count { it.windHold }} · jam events: ${state.recent.count { it.jam }}")
                    Text("Camera statistics are locally processed; no images are saved or uploaded.", style = MaterialTheme.typography.bodySmall)
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
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Daily history · last 30 available days", style = MaterialTheme.typography.titleMedium)
                if (days.isEmpty()) {
                    Text("Learning, needs more days of data")
                } else {
                    val maxSamples = days.maxOfOrNull { it.sampleCount }?.coerceAtLeast(1) ?: 1
                    val description = days.joinToString { "${dayLabel(it.dayStartMs)}: ${it.sampleCount} cycles" }
                    Canvas(Modifier.fillMaxWidth().height(160.dp).semantics { contentDescription = "Daily logged tracker cycles. $description" }) {
                        val gap = 3.dp.toPx()
                        val barWidth = ((size.width - gap * (days.size - 1)) / days.size).coerceAtLeast(1f)
                        days.forEachIndexed { index, day ->
                            val barHeight = size.height * day.sampleCount / maxSamples.toFloat()
                            drawRect(
                                color = Color(0xFFE6A11A),
                                topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                                size = Size(barWidth, barHeight)
                            )
                        }
                    }
                    days.takeLast(7).forEach { day ->
                        val sunRate = if (day.sampleCount == 0) 0 else day.sunFoundCount * 100 / day.sampleCount
                        Text("${dayLabel(day.dayStartMs)} · ${day.sampleCount} cycles · sun found $sunRate% · wind stow ${day.windHoldCycles} · jams ${day.jamCycles}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Detailed rows are retained for 90 days; older cycles are rolled into daily summaries.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun dayLabel(timestamp: Long): String = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
