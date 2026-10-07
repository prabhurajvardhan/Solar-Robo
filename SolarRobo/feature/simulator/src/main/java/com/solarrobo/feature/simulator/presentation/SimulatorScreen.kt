package com.solarrobo.feature.simulator.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solarrobo.core.contracts.DeviceHealth
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.EnvironmentSnapshot
import com.solarrobo.core.contracts.GridState
import com.solarrobo.core.contracts.HealthStatus
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.simulator.components.SimulationControl
import com.solarrobo.feature.simulator.components.SimulatorCard
import com.solarrobo.feature.simulator.domain.Scenario

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(
    viewModel: SimulatorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SimulatorScreenContent(
        state = state,
        onScenarioSelect = viewModel::selectScenario,
        onAdvanceTime = { viewModel.advanceTime(1_000L) },
        onInjectMotorJam = viewModel::triggerMotorJamFault,
        modifier = modifier
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreenContent(
    state: SimulatorUiState,
    onScenarioSelect: (Scenario) -> Unit,
    onAdvanceTime: () -> Unit,
    onInjectMotorJam: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Developer Simulator") }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.isLoading) {
                CircularProgressIndicator()
            } else if (state.isEmpty) {
                Text("No simulator snapshot is available.")
            }
            if (state.isOffline) {
                Text("Simulated device is offline.", color = MaterialTheme.colorScheme.error)
            }
            val robo = state.robo
            val energy = state.energy
            val environment = state.environment
            val health = state.deviceHealth
            if (robo != null && energy != null && environment != null && health != null) {
                SimulatorCard(robo, energy, environment, health)
            }
            SimulationControl(state.currentScenario, onScenarioSelect)
            Button(onClick = onAdvanceTime, modifier = Modifier.fillMaxWidth()) {
                Text("Advance 1 second")
            }
            Button(onClick = onInjectMotorJam, modifier = Modifier.fillMaxWidth()) {
                Text("Inject motor stall")
            }
            state.safetyEvents.lastOrNull()?.let { event ->
                Text("${event.level}: ${event.code} - ${event.message}")
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SimulatorScreenContentPreview() {
    MaterialTheme {
        SimulatorScreenContent(
            state = SimulatorUiState(
                currentScenario = Scenario.SUNNY_NORMAL,
                robo = RoboSnapshot("SIM-01", "Simulated Robo", true, RoboMode.NORMAL, 35f, 45f, 320f, 92f, 0L),
                energy = EnergySnapshot(320f, 45f, 92f, 275f, 20f, GridState.EXPORT_READY, 0L),
                environment = EnvironmentSnapshot(26f, 45f, 85_000f, 3.5f, false, 40f, 0L),
                deviceHealth = DeviceHealth(
                    HealthStatus.HEALTHY,
                    HealthStatus.HEALTHY,
                    100f,
                    HealthStatus.HEALTHY,
                    HealthStatus.HEALTHY,
                    HealthStatus.HEALTHY,
                    26f,
                    emptyList(),
                    0L
                ),
                safetyEvents = listOf(SafetyEvent("SIM-EVENT-000001", SafetyLevel.FAULT, "MOTOR_STALL_01", "Simulated motor stall.", 0L)),
                isLoading = false
            ),
            onScenarioSelect = {},
            onAdvanceTime = {},
            onInjectMotorJam = {}
        )
    }
}