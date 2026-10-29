package com.solarrobo.feature.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solarrobo.core.contracts.AccessMode
import com.solarrobo.core.ui.theme.SolarRoboTheme
import com.solarrobo.core.ui.theme.LocalAccessMode

@Composable
fun SecuritySettingsScreen(viewModel: AuthViewModel, onLockNow: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SecuritySettingsContent(
        state = state,
        onChangePin = viewModel::changePin,
        onBiometric = viewModel::setBiometricEnabled,
        onAutoLock = viewModel::setAutoLockMinutes,
        onReset = viewModel::resetProfile,
        onLockNow = onLockNow,
        modifier = modifier
    )
}

@Composable
fun SecuritySettingsContent(
    state: AuthUiState,
    onChangePin: (String, String, String) -> Unit,
    onBiometric: (Boolean) -> Unit,
    onAutoLock: (Int) -> Unit,
    onReset: (String) -> Unit,
    onLockNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOperator = LocalAccessMode.current == AccessMode.OPERATOR
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var resetPin by remember { mutableStateOf("") }
    var showReset by remember { mutableStateOf(false) }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Security", style = MaterialTheme.typography.headlineMedium)
            if (!isOperator) Text("Viewer mode · security settings are read-only until Operator unlock.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Profile: ${state.profile?.displayName ?: "Not available"}")
            Text("PIN change")
            OutlinedTextField(currentPin, { currentPin = it.filter(Char::isDigit).take(12) }, enabled = isOperator, label = { Text("Current PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(newPin, { newPin = it.filter(Char::isDigit).take(12) }, enabled = isOperator, label = { Text("New PIN (6–12 digits)") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(confirmPin, { confirmPin = it.filter(Char::isDigit).take(12) }, enabled = isOperator, label = { Text("Confirm new PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { onChangePin(currentPin, newPin, confirmPin); currentPin = ""; newPin = ""; confirmPin = "" }, enabled = isOperator && currentPin.length >= 6 && newPin.length >= 6 && newPin == confirmPin) { Text("Change PIN") }
            state.pinMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Text("Device authentication")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Allow biometric or device credential")
                Switch(checked = state.profile?.biometricEnabled == true, onCheckedChange = onBiometric, enabled = isOperator)
            }
            Text("Auto-lock after app is backgrounded")
            listOf(1, 5, 15).forEach { minutes ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    RadioButton(selected = state.profile?.autoLockMinutes == minutes, onClick = { onAutoLock(minutes) }, enabled = isOperator)
                    Text("$minutes minute${if (minutes == 1) "" else "s"}", modifier = Modifier.padding(top = 12.dp))
                }
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(onClick = onLockNow, modifier = Modifier.fillMaxWidth()) { Text("Lock now") }
            Button(onClick = { showReset = true }, enabled = isOperator, modifier = Modifier.fillMaxWidth()) { Text("Reset profile and erase local data") }
        }
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false; resetPin = "" },
            title = { Text("Erase local data?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This clears the local profile, saved preferences, and local database rows. This action cannot be undone.")
                    OutlinedTextField(resetPin, { resetPin = it.filter(Char::isDigit).take(12) }, label = { Text("Confirm with PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = { onReset(resetPin); showReset = false; resetPin = "" }, enabled = resetPin.length >= 6) { Text("Erase data") }
            },
            dismissButton = { TextButton(onClick = { showReset = false; resetPin = "" }) { Text("Cancel") } }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SecuritySettingsPreview() {
    SolarRoboTheme {
        SecuritySettingsContent(AuthUiState(), { _, _, _ -> }, {}, {}, {}, {})
    }
}
