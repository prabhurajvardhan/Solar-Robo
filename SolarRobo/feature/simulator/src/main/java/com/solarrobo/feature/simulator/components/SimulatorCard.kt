package com.solarrobo.feature.simulator.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.DeviceHealth
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.EnvironmentSnapshot
import com.solarrobo.core.contracts.GridState
import com.solarrobo.core.contracts.HealthStatus
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.RoboSnapshot

@Composable
fun SimulatorCard(
    robo: RoboSnapshot,
    energy: EnergySnapshot,
    environment: EnvironmentSnapshot,
    health: DeviceHealth,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Simulated output", style = MaterialTheme.typography.titleMedium)
            Text("Device: ${robo.name} (${if (robo.connected) "online" else "offline"})")
            Text("Mode: ${robo.mode}  |  Panel: ${robo.panelAngleDeg}° / ${robo.targetAngleDeg}°")
            Text("Generation: ${energy.generatedWatts} W  |  Battery: ${energy.batteryPercent}%")
            Text("Light: ${environment.lightLux} lux  |  Wind: ${environment.windSpeedMps} m/s")
            Text("Rain: ${if (environment.rainDetected) "detected" else "none"}  |  Motor: ${health.motorStatus}")
            Text("Simulated time: ${robo.timestamp} ms", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SimulatorCardPreview() {
    MaterialTheme {
        SimulatorCard(
            robo = RoboSnapshot("SIM-01", "Simulated Robo", true, RoboMode.NORMAL, 35f, 45f, 320f, 92f, 0L),
            energy = EnergySnapshot(320f, 45f, 92f, 275f, 20f, GridState.EXPORT_READY, 0L),
            environment = EnvironmentSnapshot(26f, 45f, 85_000f, 3.5f, false, 40f, 0L),
            health = DeviceHealth(
                HealthStatus.HEALTHY,
                HealthStatus.HEALTHY,
                100f,
                HealthStatus.HEALTHY,
                HealthStatus.HEALTHY,
                HealthStatus.HEALTHY,
                26f,
                emptyList(),
                0L
            )
        )
    }
}