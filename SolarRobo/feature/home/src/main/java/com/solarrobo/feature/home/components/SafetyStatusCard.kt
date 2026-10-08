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
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.contracts.SafetyLevel

@Composable
fun SafetyStatusCard(
    safetyLevel: SafetyLevel,
    statusMessage: String,
    alerts: List<SafetyEvent>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCritical = safetyLevel == SafetyLevel.FAULT || safetyLevel == SafetyLevel.EMERGENCY
    val isWarning = safetyLevel == SafetyLevel.PROTECTING || safetyLevel == SafetyLevel.CAUTION

    val containerColor = when {
        isCritical -> MaterialTheme.colorScheme.errorContainer
        isWarning -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isCritical -> MaterialTheme.colorScheme.onErrorContainer
        isWarning -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
                    text = "System Safety Gate",
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isCritical -> MaterialTheme.colorScheme.error
                        isWarning -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                ) {
                    Text(
                        text = safetyLevel.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )

            if (alerts.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                alerts.take(2).forEach { alert ->
                    Text(
                        text = "• [${alert.code}] ${alert.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SafetyStatusCardPreviewNormal() {
    MaterialTheme {
        SafetyStatusCard(
            safetyLevel = SafetyLevel.NORMAL,
            statusMessage = "All systems operational within safety limits.",
            alerts = emptyList(),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SafetyStatusCardPreviewFault() {
    MaterialTheme {
        SafetyStatusCard(
            safetyLevel = SafetyLevel.FAULT,
            statusMessage = "Motor stall detected on horizontal axis.",
            alerts = listOf(
                SafetyEvent("1", SafetyLevel.FAULT, "AZIMUTH_STALL", "Mechanical resistance detected on axis.", 1700000000000L)
            ),
            onClick = {}
        )
    }
}
