package com.solarrobo.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.solarrobo.core.ui.components.ViewerAccessTooltip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun GeneralSettings(
    darkTheme: Boolean,
    temperatureUnitCelsius: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit,
    onTemperatureUnitToggle: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    SettingsCard(title = "General", modifier = modifier) {
        PreferenceSwitchRow(
            title = "Dark theme",
            description = "Use a dark appearance",
            checked = darkTheme,
            onCheckedChange = onDarkThemeToggle,
            enabled = enabled
        )
        PreferenceSwitchRow(
            title = "Temperature in Celsius",
            description = "Turn off to display Fahrenheit",
            checked = temperatureUnitCelsius,
            onCheckedChange = onTemperatureUnitToggle,
            enabled = enabled
        )
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        ViewerAccessTooltip(enabled) { tooltipModifier ->
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = tooltipModifier.semantics { contentDescription = title }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GeneralSettingsPreview() {
    MaterialTheme {
        GeneralSettings(
            darkTheme = true,
            temperatureUnitCelsius = true,
            onDarkThemeToggle = {},
            onTemperatureUnitToggle = {}
        )
    }
}
