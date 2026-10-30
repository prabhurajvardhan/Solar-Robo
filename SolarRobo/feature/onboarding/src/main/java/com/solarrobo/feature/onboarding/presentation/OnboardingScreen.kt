package com.solarrobo.feature.onboarding.presentation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    var permissionsRequested by remember { mutableStateOf(false) }
    var cameraPermissionWarning by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        permissionsRequested = true
        if (results[Manifest.permission.CAMERA] != true) {
            cameraPermissionWarning = "Camera permission was not granted. You can verify Wi-Fi now, but camera tracking will remain unavailable until permission is granted."
        }
        viewModel.continueToWifiSetup()
    }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("ASTRA setup", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("Sensorless, single-axis solar tracker · local Wi-Fi only", style = MaterialTheme.typography.titleMedium)
            if (!permissionsRequested) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1 · Protect the camera and mount", style = MaterialTheme.typography.titleMedium)
                        Text("Secure the phone and place a proper dark solar filter over the camera lens before aiming toward the sun.")
                        Text("Never point an unfiltered lens at the sun. Do not look at the sun through the camera or leave the phone unattended while aimed at it.")
                        Text("Keep hands and cables clear of the panel and motor. The ESP32 has no angle encoder or weather sensors; displayed angles are estimates.", style = MaterialTheme.typography.bodySmall)
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
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("Acknowledge safety and continue")
                }
            } else {
                cameraPermissionWarning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                when (state.step) {
                    OnboardingStep.SOLAR_SAFETY -> Unit
                    OnboardingStep.WIFI_LINK -> {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("2 · Join and verify the local Wi-Fi link", style = MaterialTheme.typography.titleMedium)
                                Text("Tap below to open Android's network approval prompt for the ESP32 access point. ASTRA then checks the controller's /status response. No motor command is sent.")
                                Text("Network name: SolarTracker")
                                Text("Firmware default password: sun12345. Change it in the supplied firmware before deploying outside a trusted location.", style = MaterialTheme.typography.bodySmall)
                                Text("The ESP32 network has no internet access. Android may ask whether to stay connected to a network without internet; choose to stay connected.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Button(
                            onClick = viewModel::verifyWifiLink,
                            enabled = !state.isConnecting,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        ) { Text("Join SolarTracker and verify status") }
                        OutlinedButton(onClick = onOnboardingFinished, modifier = Modifier.fillMaxWidth()) {
                            Text("Skip for now")
                        }
                    }
                    OnboardingStep.VERIFICATION -> {
                        CircularProgressIndicator()
                        Text("Waiting for Android Wi-Fi approval and verifying the ESP32 status response. No movement is requested.")
                    }
                    OnboardingStep.COMPLETED -> {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Local link verified", style = MaterialTheme.typography.titleLarge)
                                Text("ASTRA received a controller status response. No motor movement was requested.")
                                Text("Set the installation latitude and longitude in Settings before starting tracking. Confirm the panel is physically flat before setting Home and calibrating.")
                                Text("Camera observations are estimates; physical wind, temperature, battery, grid output and panel angle are not measured.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Button(onClick = onOnboardingFinished, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text("Go to Today")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingSafetyPreview() {
    SolarRoboTheme {
        Surface { Text("ASTRA setup · filter the camera before aiming at the sun", modifier = Modifier.padding(16.dp)) }
    }
}
