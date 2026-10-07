package com.solarrobo.feature.onboarding.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solarrobo.feature.onboarding.components.OnboardingCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var deviceName by remember { mutableStateOf("My Solar Robo") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Connect Solar Robo") }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            state.errorMessage?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            when (state.step) {
                OnboardingStep.DISCOVERY -> {
                    Button(
                        onClick = { viewModel.startScanning() },
                        enabled = !state.isScanning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isScanning) "Scanning Bluetooth..." else "Scan for Nearby Devices")
                    }
                    Spacer(Modifier.height(16.dp))
                    if (state.isScanning) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.devices) { dev ->
                                OnboardingCard(
                                    candidate = dev,
                                    onClick = { viewModel.selectDevice(dev) }
                                )
                            }
                        }
                    }
                }
                OnboardingStep.CONFIGURATION -> {
                    Text(
                        text = "Configuring: ${state.selectedDevice?.name ?: "Solar Robo"}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        label = { Text("Device Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = wifiSsid,
                        onValueChange = { wifiSsid = it },
                        label = { Text("Wi-Fi SSID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = wifiPass,
                        onValueChange = { wifiPass = it },
                        label = { Text("Wi-Fi Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.confirmOnboarding(deviceName, wifiSsid, wifiPass) },
                        enabled = !state.isConnecting && deviceName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isConnecting) "Verifying Handshake..." else "Save & Pair Device")
                    }
                }
                OnboardingStep.VERIFICATION -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                OnboardingStep.COMPLETED -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Solar Robo Connected!", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Tracker is paired and ready for telemetry monitoring.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onOnboardingFinished, modifier = Modifier.fillMaxWidth()) {
                            Text("Go to Command Center")
                        }
                    }
                }
            }
        }
    }
}
