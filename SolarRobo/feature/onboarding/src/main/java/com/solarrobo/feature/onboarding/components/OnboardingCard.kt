package com.solarrobo.feature.onboarding.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.onboarding.domain.DeviceCandidate

@Composable
fun OnboardingCard(
    candidate: DeviceCandidate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = candidate.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Hardware ID: ${candidate.id}  •  RSSI: ${candidate.rssi} dBm",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingCardPreview() {
    MaterialTheme {
        OnboardingCard(
            candidate = DeviceCandidate("ROBO-ESP32-01", "Solar Tracker Rooftop", -55),
            onClick = {}
        )
    }
}
