package com.solarrobo.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.GridState

@Composable
fun EnergySummaryCard(
    energy: EnergySnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val netPower = energy.generatedWatts - energy.consumedWatts

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Energy Overview",
                    style = MaterialTheme.typography.titleMedium
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (energy.gridState) {
                        GridState.EXPORT_READY -> MaterialTheme.colorScheme.primaryContainer
                        GridState.IMPORTING -> MaterialTheme.colorScheme.secondaryContainer
                        GridState.ISOLATED -> MaterialTheme.colorScheme.tertiaryContainer
                        GridState.UNKNOWN -> MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                ) {
                    Text(
                        text = energy.gridState.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Solar Generation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.0f W".format(energy.generatedWatts),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Consumption",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.0f W".format(energy.consumedWatts),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Net Power",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%+.0f W".format(netPower),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (netPower >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Battery: %.0f%% (Reserve %.0f%%)".format(energy.batteryPercent, energy.reservePercent),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Tap to view full telemetry →",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EnergySummaryCardPreview() {
    MaterialTheme {
        EnergySummaryCard(
            energy = EnergySnapshot(
                generatedWatts = 320.0f,
                consumedWatts = 85.0f,
                batteryPercent = 94.0f,
                batteryPowerWatts = 235.0f,
                reservePercent = 20.0f,
                gridState = GridState.EXPORT_READY,
                timestamp = 1700000000000L
            ),
            onClick = {}
        )
    }
}
