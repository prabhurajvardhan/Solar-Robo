package com.solarrobo.feature.safety.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.safety.components.SafetyCard

@Composable
fun SafetyScreen(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    SafetyContent(
        state = state,
        onEmergencyStop = viewModel::emergencyStop,
        onSafePosition = viewModel::safePosition,
        onAcknowledge = viewModel::acknowledge,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SafetyContent(
    state: SafetyUiState,
    onEmergencyStop: () -> Unit,
    onSafePosition: () -> Unit,
    onAcknowledge: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Safety & Protection") }) },
        modifier = modifier
    ) { padding ->
        SafetyCard(
            state = state,
            onEmergencyStop = onEmergencyStop,
            onSafePosition = onSafePosition,
            onAcknowledge = onAcknowledge,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SafetyScreenPreview() {
    MaterialTheme {
        SafetyContent(
            state = SafetyUiState(isLoading = false),
            onEmergencyStop = {},
            onSafePosition = {},
            onAcknowledge = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SafetyScreenLoadingPreview() {
    MaterialTheme {
        SafetyContent(
            state = SafetyUiState(),
            onEmergencyStop = {},
            onSafePosition = {},
            onAcknowledge = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SafetyScreenErrorPreview() {
    MaterialTheme {
        SafetyContent(
            state = SafetyUiState(
                isLoading = false,
                currentLevel = SafetyLevel.FAULT,
                error = "Safety status unavailable."
            ),
            onEmergencyStop = {},
            onSafePosition = {},
            onAcknowledge = {}
        )
    }
}
