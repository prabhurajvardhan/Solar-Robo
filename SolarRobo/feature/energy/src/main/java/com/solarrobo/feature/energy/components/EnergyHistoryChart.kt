package com.solarrobo.feature.energy.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.EnergyHistoryPoint

@Composable
fun EnergyHistoryChart(
    history: List<EnergyHistoryPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Yield History",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (history.isEmpty()) {
                Text(
                    text = "No historical telemetry recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxWh = (history.maxOfOrNull { it.generatedWh } ?: 1.0f).coerceAtLeast(1.0f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    history.forEach { point ->
                        val fraction = (point.generatedWh / maxWh).coerceIn(0.05f, 1.0f)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${point.generatedWh.toInt()}Wh",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(fraction)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.extraSmall
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "pk:${point.peakWatts.toInt()}W",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EnergyHistoryChartPreview() {
    MaterialTheme {
        EnergyHistoryChart(
            history = listOf(
                EnergyHistoryPoint(1L, 180f, 80f, 210f, 0.88f),
                EnergyHistoryPoint(2L, 320f, 85f, 380f, 0.92f),
                EnergyHistoryPoint(3L, 450f, 85f, 490f, 0.95f),
                EnergyHistoryPoint(4L, 420f, 85f, 440f, 0.94f)
            )
        )
    }
}
