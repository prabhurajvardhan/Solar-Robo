package com.solarrobo.feature.safety.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.SafetyDecision
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.safety.presentation.SafetyUiState

@Composable
fun SafetyCard(
    state: SafetyUiState,
    onEmergencyStop: () -> Unit,
    onSafePosition: () -> Unit,
    onAcknowledge: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Safety status: ${state.currentLevel}", style = MaterialTheme.typography.titleLarge)
            if (state.isLoading) Text("Loading safety status...")
            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            state.lastDecision?.let { decision ->
                Text(
                    when (decision) {
                        SafetyDecision.Allow -> "Command allowed."
                        is SafetyDecision.Block -> "Command blocked: ${decision.reason}"
                        is SafetyDecision.Modify -> "Command modified: ${decision.reason}"
                    }
                )
            }
            state.lastCommandResult?.let { result ->
                Text(
                    if (result.accepted) "Safety command accepted."
                    else result.reason ?: "Safety command rejected.",
                    color = if (result.accepted) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.error
                )
            }
            if (state.incidents.isEmpty() && !state.isLoading) {
                Text("No active safety incidents.")
            } else {
                state.incidents.forEach { event ->
                    IncidentRow(event = event, onAcknowledge = onAcknowledge)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onEmergencyStop, modifier = Modifier.weight(1f)) {
                    Text("Emergency stop")
                }
                OutlinedButton(onClick = onSafePosition, modifier = Modifier.weight(1f)) {
                    Text("Safe position")
                }
            }
        }
    }
}

@Composable
private fun IncidentRow(
    event: SafetyEvent,
    onAcknowledge: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${event.code}: ${event.message}", style = MaterialTheme.typography.bodyMedium)
            Text(event.level.name, style = MaterialTheme.typography.labelSmall)
        }
        if (!event.acknowledged) {
            OutlinedButton(onClick = { onAcknowledge(event.id) }) {
                Text("Acknowledge")
            }
        } else {
            Text("Acknowledged", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SafetyCardPreview() {
    MaterialTheme {
        SafetyCard(
            state = SafetyUiState(isLoading = false),
            onEmergencyStop = {},
            onSafePosition = {},
            onAcknowledge = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SafetyCardEmergencyPreview() {
    MaterialTheme {
        SafetyCard(
            state = SafetyUiState(
                isLoading = false,
                currentLevel = SafetyLevel.EMERGENCY,
                incidents = listOf(
                    SafetyEvent("event-1", SafetyLevel.EMERGENCY, "E_STOP", "Emergency stop requested.", 1L)
                )
            ),
            onEmergencyStop = {},
            onSafePosition = {},
            onAcknowledge = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IncidentRowPreview() {
    MaterialTheme {
        IncidentRow(
            event = SafetyEvent("event-1", SafetyLevel.CAUTION, "COMMAND_BLOCKED", "Command blocked.", 1L),
            onAcknowledge = {}
        )
    }
}
