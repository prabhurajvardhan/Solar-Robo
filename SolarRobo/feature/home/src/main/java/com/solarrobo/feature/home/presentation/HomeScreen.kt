package com.solarrobo.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.GridState
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.home.components.EnergySummaryCard
import com.solarrobo.feature.home.components.QuickActionsCard
import com.solarrobo.feature.home.components.RoboStatusCard
import com.solarrobo.feature.home.components.SafetyStatusCard

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEnergy: () -> Unit,
    onNavigateToControl: () -> Unit = {},
    onNavigateToSafety: () -> Unit = {},
    onNavigateToCamera: () -> Unit = {},
    onNavigateToSimulator: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        state = state,
        onRefresh = viewModel::refresh,
        onNavigateToEnergy = onNavigateToEnergy,
        onNavigateToControl = onNavigateToControl,
        onNavigateToSafety = onNavigateToSafety,
        onNavigateToCamera = onNavigateToCamera,
        onNavigateToSimulator = onNavigateToSimulator,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onNavigateToEnergy: () -> Unit,
    onNavigateToControl: () -> Unit,
    onNavigateToSafety: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToSimulator: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Command Center") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh telemetry"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (state) {
                is HomeUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Loading Solar Robo command center...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is HomeUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text("Retry")
                        }
                    }
                }

                is HomeUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is HomeUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SafetyStatusCard(
                            safetyLevel = state.safetyLevel,
                            statusMessage = state.statusMessage,
                            alerts = state.activeAlerts,
                            onClick = onNavigateToSafety
                        )

                        RoboStatusCard(
                            robo = state.robo,
                            onClick = onNavigateToControl
                        )

                        EnergySummaryCard(
                            energy = state.energy,
                            onClick = onNavigateToEnergy
                        )

                        QuickActionsCard(
                            onNavigateToEnergy = onNavigateToEnergy,
                            onNavigateToCamera = onNavigateToCamera,
                            onNavigateToSimulator = onNavigateToSimulator
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreviewSuccess() {
    MaterialTheme {
        HomeScreenContent(
            state = HomeUiState.Success(
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
                energy = EnergySnapshot(
                    generatedWatts = 320.0f,
                    consumedWatts = 85.0f,
                    batteryPercent = 94.0f,
                    batteryPowerWatts = 235.0f,
                    reservePercent = 20.0f,
                    gridState = GridState.EXPORT_READY,
                    timestamp = 1700000000000L
                ),
                safetyLevel = SafetyLevel.NORMAL,
                activeAlerts = emptyList(),
                statusMessage = "All tracker and energy systems nominal."
            ),
            onRefresh = {},
            onNavigateToEnergy = {},
            onNavigateToControl = {},
            onNavigateToSafety = {},
            onNavigateToCamera = {},
            onNavigateToSimulator = {}
        )
    }
}
