package com.solarrobo.feature.settings.presentation

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.solarrobo.feature.settings.components.GeneralSettings
import com.solarrobo.feature.settings.components.NotificationSettings
import com.solarrobo.feature.settings.components.VoiceSettings
import com.solarrobo.feature.settings.domain.SettingsPatch
import com.solarrobo.feature.settings.domain.UserSettings
import com.solarrobo.core.contracts.AccessMode
import com.solarrobo.core.ui.theme.LocalAccessMode
import com.solarrobo.core.ui.components.ViewerAccessTooltip

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToSecurity: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreenContent(
        state = state,
        onUpdate = viewModel::update,
        onReset = viewModel::reset,
        onRetry = viewModel::retry,
        onSecurity = onNavigateToSecurity,
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
    onSecurity: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isOperator = LocalAccessMode.current == AccessMode.OPERATOR
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
            if (!isOperator) Text("Viewer mode · settings are read-only until Operator unlock.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onSecurity, modifier = Modifier.fillMaxWidth()) { Text("Security & access") }
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
                    onReset = onReset,
                    isOperator = isOperator
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    settings: UserSettings,
    onUpdate: (SettingsPatch) -> Unit,
    onReset: () -> Unit,
    isOperator: Boolean
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
                },
                enabled = isOperator
            )
            NotificationSettings(
                enabled = settings.notificationsEnabled,
                highWindAlertsEnabled = settings.highWindAlertsEnabled,
                onToggle = { onUpdate(SettingsPatch(notificationsEnabled = it)) },
                onHighWindAlertsToggle = {
                    onUpdate(SettingsPatch(highWindAlertsEnabled = it))
                },
                controlsEnabled = isOperator
            )
            VoiceSettings(
                enabled = settings.voiceFeedbackEnabled,
                onToggle = { onUpdate(SettingsPatch(voiceFeedbackEnabled = it)) },
                controlsEnabled = isOperator
            )
            TrackerLocationSettings(
                latitudeDeg = settings.latitudeDeg,
                longitudeDeg = settings.longitudeDeg,
                onSave = { latitude, longitude -> onUpdate(SettingsPatch(latitudeDeg = latitude, longitudeDeg = longitude)) },
                enabled = isOperator
            )
            BatteryOptimizationSettings(enabled = isOperator)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("About ASTRA", style = MaterialTheme.typography.titleMedium)
                    Text("ASTRA v2.0.0-astra", style = MaterialTheme.typography.bodyLarge)
                    Text("Sensorless, single-axis solar tracking · offline-first", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        ViewerAccessTooltip(isOperator) { actionModifier ->
            OutlinedButton(
                onClick = onReset,
                enabled = isOperator,
                modifier = actionModifier.fillMaxWidth()
            ) {
                Text("Restore defaults")
            }
        }
    }
}

@Composable
private fun TrackerLocationSettings(
    latitudeDeg: Double?,
    longitudeDeg: Double?,
    onSave: (Double, Double) -> Unit,
    enabled: Boolean
) {
    var latitude by remember(latitudeDeg) { mutableStateOf(latitudeDeg?.toString().orEmpty()) }
    var longitude by remember(longitudeDeg) { mutableStateOf(longitudeDeg?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tracker location", style = MaterialTheme.typography.titleMedium)
        Text("Used for the sun-position calculation; coordinates stay on this device.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(latitude, { latitude = it }, enabled = enabled, label = { Text("Latitude (−90 to 90)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(longitude, { longitude = it }, enabled = enabled, label = { Text("Longitude (−180 to 180)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        ViewerAccessTooltip(enabled) { actionModifier -> Button(onClick = {
            val lat = latitude.toDoubleOrNull()
            val lon = longitude.toDoubleOrNull()
            error = when {
                lat == null || lon == null -> "Enter both coordinates as decimal degrees."
                lat !in -90.0..90.0 || lon !in -180.0..180.0 -> "Coordinates are outside the valid range."
                else -> null
            }
            if (error == null) onSave(lat!!, lon!!)
        }, enabled = enabled, modifier = actionModifier.fillMaxWidth()) { Text("Save tracker location") } }
    }
}

@Composable
private fun BatteryOptimizationSettings(enabled: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var exempt by remember { mutableStateOf(false) }
    fun refresh() {
        exempt = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        refresh()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Background tracking", style = MaterialTheme.typography.titleMedium)
        Text(
            if (exempt) "Battery optimization exemption is enabled."
            else "Battery optimization may pause background tracking. Exemption is currently not enabled.",
            style = MaterialTheme.typography.bodySmall
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !exempt) {
            ViewerAccessTooltip(enabled) { actionModifier -> OutlinedButton(onClick = {
                val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                runCatching { context.startActivity(request) }.onFailure {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }, enabled = enabled, modifier = actionModifier.fillMaxWidth()) { Text("Review battery-optimization setting") } }
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
