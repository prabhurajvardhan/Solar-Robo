package com.solarrobo.feature.simulator.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.simulator.domain.Scenario

@Composable
fun SimulationControl(
    currentScenario: Scenario,
    onScenarioSelect: (Scenario) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Scenario", style = MaterialTheme.typography.titleMedium)
            Scenario.entries.forEach { scenario ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.RadioButton) { onScenarioSelect(scenario) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = currentScenario == scenario,
                        onClick = { onScenarioSelect(scenario) }
                    )
                    Text(scenario.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SimulationControlPreview() {
    MaterialTheme {
        SimulationControl(currentScenario = Scenario.SUNNY_NORMAL, onScenarioSelect = {})
    }
}