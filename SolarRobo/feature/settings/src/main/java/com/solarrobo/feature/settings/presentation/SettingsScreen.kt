package com.solarrobo.feature.settings.presentation

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
import androidx.compose.material3.OutlinedButton
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
import com.solarrobo.feature.settings.components.GeneralSettings
import com.solarrobo.feature.settings.components.NotificationSettings
import com.solarrobo.feature.settings.components.VoiceSettings
import com.solarrobo.feature.settings.domain.SettingsPatch
import com.solarrobo.feature.settings.domain.UserSettings

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreenContent(
        state = state,
        onUpdate = viewModel::update,
        onReset = viewModel::reset,
        onRetry = viewModel::retry,
        modifier = modifier
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    state: SettingsUiState,
    onUpdate: (SettingsPatch) -> Unit,
    onReset: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings & Preferences") }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Preferences are encrypted and stored on this device. They remain available offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
            when {
                state.isLoading -> LoadingState()
                state.settings == null && state.errorMessage != null -> Unit
                state.settings == null -> EmptyState()
                else -> SettingsContent(
                    settings = state.settings,
                    onUpdate = onUpdate,
                    onReset = onReset
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    settings: UserSettings,
    onUpdate: (SettingsPatch) -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GeneralSettings(
                darkTheme = settings.darkTheme,
                temperatureUnitCelsius = settings.temperatureUnitCelsius,
                onDarkThemeToggle = { onUpdate(SettingsPatch(darkTheme = it)) },
                onTemperatureUnitToggle = {
                    onUpdate(SettingsPatch(temperatureUnitCelsius = it))
                }
            )
            NotificationSettings(
                enabled = settings.notificationsEnabled,
                highWindAlertsEnabled = settings.highWindAlertsEnabled,
                onToggle = { onUpdate(SettingsPatch(notificationsEnabled = it)) },
                onHighWindAlertsToggle = {
                    onUpdate(SettingsPatch(highWindAlertsEnabled = it))
                }
            )
            VoiceSettings(
                enabled = settings.voiceFeedbackEnabled,
                onToggle = { onUpdate(SettingsPatch(voiceFeedbackEnabled = it)) }
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Restore defaults")
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text("Loading settings...", modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No settings available", style = MaterialTheme.typography.titleMedium)
        Text(
            "Try loading your preferences again.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    MaterialTheme {
        SettingsScreenContent(
            state = SettingsUiState(settings = UserSettings()),
            onUpdate = {},
            onReset = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsLoadingPreview() {
    MaterialTheme {
        SettingsScreenContent(
            state = SettingsUiState(isLoading = true),
            onUpdate = {},
            onReset = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsErrorPreview() {
    MaterialTheme {
        SettingsScreenContent(
            state = SettingsUiState(errorMessage = "Preferences could not be loaded."),
            onUpdate = {},
            onReset = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsOfflinePreview() {
    MaterialTheme {
        SettingsScreenContent(
            state = SettingsUiState(settings = UserSettings(), isOffline = true),
            onUpdate = {},
            onReset = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsEmptyPreview() {
    MaterialTheme {
        SettingsScreenContent(
            state = SettingsUiState(),
            onUpdate = {},
            onReset = {},
            onRetry = {}
        )
    }
}
