package com.solarrobo.feature.onboarding.presentation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solarrobo.core.ui.theme.SolarRoboTheme

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var started by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf("My Solar Robo") }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        started = true
        viewModel.startScanning()
    }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("ASTRA", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("Sensorless single-axis solar tracker", style = MaterialTheme.typography.titleMedium)
            Text("The phone estimates the sun direction and panel angle. The ESP32 drives only the motor. Core operation uses local Wi-Fi; no internet is required.")
            if (!started) {
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Before you begin", style = MaterialTheme.typography.titleMedium)
                        Text("Mount the phone securely. Put a proper dark solar filter in front of the camera. Never leave a bare phone lens facing the sun.")
                        Text("Your installation latitude and longitude are required before tracking can be calibrated.")
                    }
                }
                Button(onClick = {
                    val requested = buildList {
                        add(Manifest.permission.CAMERA)
                        if (Build.VERSION.SDK_INT >= 33) {
                            add(Manifest.permission.NEARBY_WIFI_DEVICES)
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            add(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }.toTypedArray()
                    permissionLauncher.launch(requested)
                }, modifier = Modifier.fillMaxWidth()) { Text("Set up tracker") }
            } else {
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                when (state.step) {
                    OnboardingStep.DISCOVERY -> {
                        Text("Join the controller access point")
                        Text("Choose SolarTracker. Android will show a system prompt to join its local Wi-Fi network.")
                        if (state.isScanning) CircularProgressIndicator()
                        state.devices.forEach { device ->
                            OutlinedButton(onClick = { viewModel.selectDevice(device) }, modifier = Modifier.fillMaxWidth()) { Text(device.name) }
                        }
                    }
                    OnboardingStep.CONFIGURATION -> {
                        OutlinedTextField(deviceName, { deviceName = it }, label = { Text("Device name") }, modifier = Modifier.fillMaxWidth())
                        Text("Wi-Fi name: SolarTracker")
                        Text("Firmware default password: sun12345. Change it in firmware before deploying outside a trusted location.")
                        Text("Next, enter latitude and longitude in Settings. Tracking remains disabled until those required values and calibration are complete.")
                        Text("Lens safety: use a dark solar filter; do not aim an unfiltered camera at the sun.")
                        Button(onClick = { viewModel.confirmOnboarding(deviceName, "SolarTracker", "sun12345") }, modifier = Modifier.fillMaxWidth(), enabled = !state.isConnecting) {
                            if (state.isConnecting) CircularProgressIndicator() else Text("Join Wi-Fi & verify")
                        }
                    }
                    OnboardingStep.VERIFICATION -> CircularProgressIndicator()
                    OnboardingStep.COMPLETED -> {
                        Text("Controller link verified", style = MaterialTheme.typography.titleLarge)
                        Text("No physical sensor telemetry is available. Angles and camera observations are estimates.")
                        Button(onClick = onOnboardingFinished, modifier = Modifier.fillMaxWidth()) { Text("Go to Today") }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingLandingPreview() {
    SolarRoboTheme {
        Surface { Row(Modifier.padding(16.dp)) { Text("ASTRA setup preview") } }
    }
}
