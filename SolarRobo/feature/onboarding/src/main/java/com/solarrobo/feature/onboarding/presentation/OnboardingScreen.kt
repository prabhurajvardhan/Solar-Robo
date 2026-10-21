package com.solarrobo.feature.onboarding.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarrobo.core.ui.theme.ForestInk
import com.solarrobo.core.ui.theme.Porcelain
import com.solarrobo.core.ui.theme.Sage
import com.solarrobo.core.ui.theme.SolarCitron
import com.solarrobo.feature.onboarding.R

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onOnboardingFinished: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    var started by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf("My Solar Robo") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }
    Surface(modifier.fillMaxSize(), color = Porcelain) {
        if (!started && state.step == OnboardingStep.DISCOVERY) {
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(50)).background(SolarCitron))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("SOLAR ROBO", color = ForestInk, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Text("TRACK  •  ANALYZE  •  PROTECT", color = Sage, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                }
                Spacer(Modifier.height(46.dp))
                Text("Your solar system,", color = ForestInk, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                Text("understood.", color = Sage, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("Track performance. Catch issues early.\nStay in control.", color = ForestInk.copy(alpha = .7f), style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(22.dp))
                Surface(color = SolarCitron.copy(alpha = .55f), shape = RoundedCornerShape(50), modifier = Modifier.align(Alignment.Start)) {
                    Text("●  READY TO CONNECT", color = ForestInk, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                }
                Spacer(Modifier.height(18.dp))
                Image(painterResource(R.drawable.solar_robo_hero), null, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { started = true; viewModel.startScanning() }, colors = ButtonDefaults.buttonColors(containerColor = ForestInk, contentColor = Porcelain), shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().height(62.dp)) { Text("Get started  →", fontSize = 19.sp) }
                Spacer(Modifier.height(14.dp))
                Text("Already have an account?  Sign in", color = ForestInk, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Text("—  YOUR SYSTEM. YOUR DATA. YOUR CONTROL.  —", color = Sage, fontSize = 10.sp, letterSpacing = 1.4.sp)
            }
        } else {
            PairingFlow(state, deviceName, { deviceName = it }, wifiSsid, { wifiSsid = it }, wifiPass, { wifiPass = it }, viewModel, onOnboardingFinished)
        }
    }
}

@Composable
private fun PairingFlow(state: OnboardingUiState, deviceName: String, onName: (String) -> Unit, ssid: String, onSsid: (String) -> Unit, pass: String, onPass: (String) -> Unit, viewModel: OnboardingViewModel, onFinished: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Connect your system", style = MaterialTheme.typography.headlineMedium, color = ForestInk, fontWeight = FontWeight.Bold)
        Text("A few quiet steps and Robo will be ready to track the sun.", color = ForestInk.copy(alpha = .7f))
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when (state.step) {
            OnboardingStep.DISCOVERY -> { Text(if (state.isScanning) "Looking for nearby devices…" else "Choose your Solar Robo controller") ; state.devices.forEach { dev -> OutlinedButton({ viewModel.selectDevice(dev) }, Modifier.fillMaxWidth()) { Text(dev.name) } } }
            OnboardingStep.CONFIGURATION -> { OutlinedTextField(deviceName, onName, label={Text("Device name")}, modifier=Modifier.fillMaxWidth()); OutlinedTextField(ssid,onSsid,label={Text("Wi-Fi network")},modifier=Modifier.fillMaxWidth()); OutlinedTextField(pass,onPass,label={Text("Wi-Fi password")},modifier=Modifier.fillMaxWidth()); Button({viewModel.confirmOnboarding(deviceName,ssid,pass)}, Modifier.fillMaxWidth()){Text("Save & pair device") } }
            OnboardingStep.VERIFICATION -> CircularProgressIndicator()
            OnboardingStep.COMPLETED -> { Text("You’re all set.", style=MaterialTheme.typography.headlineMedium); Button(onFinished, Modifier.fillMaxWidth()){Text("Go to command center")} }
        }
    }
}
