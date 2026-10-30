package com.solarrobo.feature.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.solarrobo.core.ui.theme.SolarRoboTheme

@Composable
fun CreateProfileScreen(viewModel: AuthViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CreateProfileContent(state, viewModel::createProfile, modifier)
}

@Composable
fun CreateProfileContent(
    state: AuthUiState,
    onCreate: (String, String, String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayName by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var biometric by remember { mutableStateOf(false) }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Create local profile", style = MaterialTheme.typography.headlineMedium)
            Text("This profile stays on this device. Your PIN is stored only as a salted verifier in encrypted local storage.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(displayName, { displayName = it.take(48) }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(12) }, label = { Text("Create PIN (6–12 digits)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(confirmation, { confirmation = it.filter(Char::isDigit).take(12) }, label = { Text("Confirm PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
            Text("Allow device biometric unlock", style = MaterialTheme.typography.titleMedium)
            Switch(checked = biometric, onCheckedChange = { biometric = it })
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { onCreate(displayName, pin, confirmation, biometric) },
                enabled = !state.isLoading && displayName.isNotBlank() && pin.length >= 6 && pin == confirmation,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create profile") }
            Text("No account or cloud recovery is used. Keep your PIN private.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateProfilePreview() {
    SolarRoboTheme { CreateProfileContent(AuthUiState(), { _, _, _, _ -> }) }
}
