package com.solarrobo.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun NotificationSettings(
    enabled: Boolean,
    highWindAlertsEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onHighWindAlertsToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(title = "Notifications", modifier = modifier) {
        NotificationSwitchRow(
            title = "Push notifications",
            description = "Alerts for safety events",
            checked = enabled,
            onCheckedChange = onToggle
        )
        NotificationSwitchRow(
            title = "High-wind alerts",
            description = "Notify when high winds are detected",
            checked = highWindAlertsEnabled,
            onCheckedChange = onHighWindAlertsToggle
        )
    }
}

@Composable
private fun NotificationSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = title }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationSettingsPreview() {
    MaterialTheme {
        NotificationSettings(
            enabled = true,
            highWindAlertsEnabled = true,
            onToggle = {},
            onHighWindAlertsToggle = {}
        )
    }
}
