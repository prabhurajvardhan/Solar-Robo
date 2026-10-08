package com.solarrobo.feature.energy.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.GridState

@Composable
fun EnergyFlowCard(
    generatedWatts: Float,
    consumedWatts: Float,
    netPowerWatts: Float,
    gridState: GridState,
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
                text = "Real-time Energy Flow",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Production",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${generatedWatts.toInt()} W",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        text = "Consumption",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${consumedWatts.toInt()} W",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Column {
                    Text(
                        text = "Net Flow",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (netPowerWatts >= 0) "+${netPowerWatts.toInt()} W" else "${netPowerWatts.toInt()} W",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (netPowerWatts >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Grid Connection:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (gridState) {
                        GridState.EXPORT_READY -> "Export Ready (Surplus)"
                        GridState.IMPORTING -> "Importing Power"
                        GridState.ISOLATED -> "Isolated / Islanded"
                        GridState.UNKNOWN -> "Unknown Grid State"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EnergyFlowCardPreview() {
    MaterialTheme {
        EnergyFlowCard(
            generatedWatts = 420.0f,
            consumedWatts = 85.0f,
            netPowerWatts = 335.0f,
            gridState = GridState.EXPORT_READY
        )
    }
}
