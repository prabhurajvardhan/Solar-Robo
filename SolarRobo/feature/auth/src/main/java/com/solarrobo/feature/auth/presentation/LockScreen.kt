package com.solarrobo.feature.auth.presentation

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solarrobo.core.ui.theme.SolarRoboTheme

@Composable
fun LockScreen(viewModel: AuthViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val prompt = remember(activity, viewModel) {
        activity?.let { owner ->
            BiometricPrompt(owner, ContextCompat.getMainExecutor(owner), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.unlockWithBiometric()
                }
            })
        }
    }
    LockScreenContent(
        state = state,
        onUnlock = viewModel::unlockWithPin,
        onViewer = viewModel::enterViewerMode,
        onBiometric = {
            prompt?.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock ASTRA")
                    .setSubtitle("Authenticate with your device")
                    .setAllowedAuthenticators(
                        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()
            )
        },
        modifier = modifier
    )
}

@Composable
fun LockScreenContent(
    state: AuthUiState,
    onUnlock: (String) -> Unit,
    onViewer: () -> Unit,
    onBiometric: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("ASTRA", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text("Welcome back${state.profile?.displayName?.let { ", $it" } ?: ""}", style = MaterialTheme.typography.titleLarge)
            Text("Enter your local PIN to enable operator controls.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                label = { Text("PIN (6 digits)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.pinMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Button(onClick = { onUnlock(pin); pin = "" }, enabled = pin.length == 6 && state.lockoutRemainingMs <= 0L, modifier = Modifier.fillMaxWidth()) {
                Text("Unlock as Operator")
            }
            if (state.profile?.biometricEnabled == true) {
                OutlinedButton(onClick = onBiometric, modifier = Modifier.fillMaxWidth()) { Text("Use device authentication") }
            }
            OutlinedButton(onClick = onViewer, modifier = Modifier.fillMaxWidth()) { Text("Continue as Viewer · read-only") }
            Text("Operator controls remain locked until a valid PIN or device credential is verified.", style = MaterialTheme.typography.bodySmall)
            Text("ASTRA 2.0.0-astra", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LockScreenPreview() {
    SolarRoboTheme {
        LockScreenContent(
            state = AuthUiState(), onUnlock = {}, onViewer = {}, onBiometric = {}
        )
    }
}
