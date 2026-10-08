package com.solarrobo.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun QuickActionsCard(
    onNavigateToEnergy: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToSimulator: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToEnergy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Energy")
                }

                OutlinedButton(
                    onClick = onNavigateToCamera,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Camera")
                }

                OutlinedButton(
                    onClick = onNavigateToSimulator,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Simulator")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuickActionsCardPreview() {
    MaterialTheme {
        QuickActionsCard(
            onNavigateToEnergy = {},
            onNavigateToCamera = {},
            onNavigateToSimulator = {}
        )
    }
}
