package com.solarrobo.feature.energy.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.GridState
import com.solarrobo.feature.energy.components.BatteryCard
import com.solarrobo.feature.energy.components.EnergyFlowCard
import com.solarrobo.feature.energy.components.EnergyHistoryChart
import com.solarrobo.feature.energy.components.SolarGenerationCard
import com.solarrobo.feature.energy.domain.CalculateEnergyMetrics
import com.solarrobo.feature.energy.domain.EnergyStatus
import com.solarrobo.feature.energy.domain.EnergySummaryMetrics
import com.solarrobo.feature.energy.mock.EnergyScenario
import com.solarrobo.feature.energy.mock.FakeEnergyRepository

@Composable
fun EnergyScreen(
    viewModel: EnergyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EnergyScreenContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::refresh,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyScreenContent(
    state: EnergyUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Energy Center") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when (state) {
                is EnergyUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Loading energy telemetry...")
                    }
                }
                is EnergyUiState.Error -> {
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
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
                is EnergyUiState.Empty -> {
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
                is EnergyUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SolarGenerationCard(
                            generatedWatts = state.snapshot.generatedWatts,
                            status = state.metrics.status,
                            efficiencyPercent = state.metrics.averageSolarEfficiency
                        )

                        BatteryCard(
                            batteryPercent = state.snapshot.batteryPercent,
                            batteryPowerWatts = state.snapshot.batteryPowerWatts,
                            reservePercent = state.snapshot.reservePercent,
                            isCharging = state.metrics.isCharging
                        )

                        EnergyFlowCard(
                            generatedWatts = state.snapshot.generatedWatts,
                            consumedWatts = state.snapshot.consumedWatts,
                            netPowerWatts = state.metrics.netPowerWatts,
                            gridState = state.snapshot.gridState
                        )

                        EnergyHistoryChart(
                            history = state.history
                        )

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EnergyScreenPreview() {
    val snapshot = FakeEnergyRepository.createSnapshotForScenario(EnergyScenario.NORMAL)
    val history = FakeEnergyRepository.createHistoryForScenario(EnergyScenario.NORMAL)
    val metrics = CalculateEnergyMetrics().invoke(snapshot, history)

    MaterialTheme {
        EnergyScreenContent(
            state = EnergyUiState.Success(
                snapshot = snapshot,
                history = history,
                metrics = metrics
            ),
            onBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EnergyScreenLoadingPreview() {
    MaterialTheme {
        EnergyScreenContent(
            state = EnergyUiState.Loading,
            onBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EnergyScreenErrorPreview() {
    MaterialTheme {
        EnergyScreenContent(
            state = EnergyUiState.Error("Telemetry stream interrupted."),
            onBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EnergyScreenEmptyPreview() {
    MaterialTheme {
        EnergyScreenContent(
            state = EnergyUiState.Empty(),
            onBack = {},
            onRetry = {}
        )
    }
}
