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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun BatteryCard(
    batteryPercent: Float,
    batteryPowerWatts: Float,
    reservePercent: Float,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Battery Storage",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isCharging) "Charging (+${batteryPowerWatts.toInt()} W)" else "Discharging (${batteryPowerWatts.toInt()} W)",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${batteryPercent.toInt()}%",
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = "State of Charge",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (batteryPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (batteryPercent <= reservePercent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Emergency Reserve Limit: ${reservePercent.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BatteryCardPreview() {
    MaterialTheme {
        BatteryCard(
            batteryPercent = 78.0f,
            batteryPowerWatts = 335.0f,
            reservePercent = 20.0f,
            isCharging = true
        )
    }
}
