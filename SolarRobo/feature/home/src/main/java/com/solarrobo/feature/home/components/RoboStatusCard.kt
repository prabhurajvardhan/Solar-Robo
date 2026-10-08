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
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.RoboSnapshot

@Composable
fun RoboStatusCard(
    robo: RoboSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                Column {
                    Text(
                        text = "Solar Tracker",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${robo.name} (${robo.deviceId})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (robo.connected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = if (robo.connected) "CONNECTED" else "OFFLINE",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (robo.connected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
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
                        text = "Current Angle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.1f°".format(robo.panelAngleDeg),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target Angle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.1f°".format(robo.targetAngleDeg),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (robo.mode) {
                        RoboMode.NORMAL, RoboMode.OPTIMIZING -> MaterialTheme.colorScheme.secondaryContainer
                        RoboMode.CONSERVING -> MaterialTheme.colorScheme.tertiaryContainer
                        RoboMode.PROTECTING, RoboMode.SAFE, RoboMode.FAULT -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = "Mode: ${robo.mode.name}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Gen: %.0f W  •  Battery: %.0f%%".format(robo.generationWatts, robo.batteryPercent),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoboStatusCardPreview() {
    MaterialTheme {
        RoboStatusCard(
            robo = RoboSnapshot(
                deviceId = "ROBO-01",
                name = "Solar Robo Primary",
                connected = true,
                mode = RoboMode.NORMAL,
                panelAngleDeg = 45.0f,
                targetAngleDeg = 45.0f,
                generationWatts = 320.0f,
                batteryPercent = 94.0f,
                timestamp = 1700000000000L
            ),
            onClick = {}
        )
    }
}
