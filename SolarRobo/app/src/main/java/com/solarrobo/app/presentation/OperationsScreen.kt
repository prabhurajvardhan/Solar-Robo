package com.solarrobo.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solarrobo.core.ui.components.SolarCard
import com.solarrobo.core.ui.theme.DarkBg
import com.solarrobo.core.ui.theme.SunAmber
import com.solarrobo.core.ui.theme.Olive
import com.solarrobo.core.ui.theme.SolarRoboTheme
import com.solarrobo.core.ui.theme.ErrorRed
import com.solarrobo.core.ui.theme.OliveMutedTint
import com.solarrobo.core.ui.theme.InkSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationsScreen(
    title: String,
    route: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var angle by remember { mutableFloatStateOf(36f) }
    var message by remember { mutableStateOf("Ready — every movement is checked by the Safety Gate.") }
    val isControl = route == "control"

    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SolarCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isControl) Icons.Default.Tune else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isControl) SunAmber else Olive
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Offline-ready workspace", fontWeight = FontWeight.Bold)
                        Text(
                            "Simulator-backed data is active. No cloud connection required.",
                            color = InkSecondary
                        )
                    }
                }
            }

            if (isControl) {
                SolarCard {
                    Text("Tracker target angle", fontWeight = FontWeight.Bold)
                    Text("${angle.toInt()}°", color = SunAmber, fontWeight = FontWeight.Bold)
                    Slider(
                        value = angle,
                        onValueChange = { angle = it.coerceIn(-85f, 85f) },
                        valueRange = -90f..90f
                    )
                    Text("Mechanical bounds: −85° to +85°", color = InkSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { message = "Move request staged at ${angle.toInt()}°. Safety Gate: ALLOW." },
                            colors = ButtonDefaults.buttonColors(containerColor = SunAmber, contentColor = DarkBg),
                            modifier = Modifier.weight(1f)
                        ) { Text("Stage move") }
                        OutlinedButton(
                            onClick = { angle = 0f; message = "Safe stow requested. Tracker target is now 0°." },
                            modifier = Modifier.weight(1f)
                        ) { Text("Safe stow") }
                    }
                }
            }

            SolarCard {
                Text("Safety Gate", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                GateRow("Wind protection", "Monitoring", Olive)
                GateRow("Mechanical limits", "Enforced", Olive)
                GateRow("AI command authority", "Blocked by design", OliveMutedTint)
                if (isControl) {
                    OutlinedButton(
                        onClick = { message = "Emergency stop staged locally. Hardware dispatch still requires the device adapter." },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) { Text("Emergency stop") }
                }
            }

            SolarCard {
                Text("Latest system note", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(message, color = InkSecondary)
            }

            if (!isControl) {
                val checks = when (route) {
                    "environment" -> listOf("Irradiance" to "82.4 klux", "Wind" to "3.5 m/s", "Panel temperature" to "40°C", "Rain" to "Not detected")
                    "health" -> listOf("Motors" to "Healthy", "Battery" to "98% health", "Sensors" to "Healthy", "Camera" to "Ready")
                    "activity" -> listOf("09:42" to "Tracker realigned +4°", "09:30" to "Energy peak 320 W", "08:58" to "Offline assistant ready")
                    "analytics" -> listOf("Today" to "2.84 kWh", "Efficiency" to "91%", "Peak" to "320 W", "Uptime" to "99.2%")
                    else -> listOf("Telemetry" to "Live", "Simulator" to "Connected", "Offline mode" to "Available")
                }
                checks.forEach { (label, value) ->
                    SolarCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = InkSecondary)
                            Text(value, color = SunAmber, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { 0.82f },
                            modifier = Modifier.fillMaxWidth(),
                            color = SunAmber,
                            trackColor = CardDefaults.cardColors().containerColor.copy(alpha = 0.35f)
                        )
                    }
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to Command Center") }
        }
    }
}

@Composable
private fun GateRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = InkSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = color)
            Text(value, modifier = Modifier.padding(start = 6.dp), color = color)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OperationsScreenPreview() {
    SolarRoboTheme { OperationsScreen("Tracker Control", "control", {}) }
}
