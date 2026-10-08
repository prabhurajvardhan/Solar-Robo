package com.solarrobo.app.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.core.ui.components.MetricItem
import com.solarrobo.core.ui.components.SolarCard
import com.solarrobo.core.ui.components.StatusBadge
import com.solarrobo.core.ui.theme.DarkBackground
import com.solarrobo.core.ui.theme.DarkBorder
import com.solarrobo.core.ui.theme.DarkCard
import com.solarrobo.core.ui.theme.SolarAmber
import com.solarrobo.core.ui.theme.SolarEmerald
import com.solarrobo.core.ui.theme.SolarRoboTheme
import com.solarrobo.core.ui.theme.SolarRose
import com.solarrobo.core.ui.theme.SolarSky
import com.solarrobo.core.ui.theme.TextMuted
import com.solarrobo.core.ui.theme.TextPrimary
import com.solarrobo.core.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSimulator: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    HomeScreenContent(
        state = state,
        onNavigateToSimulator = onNavigateToSimulator,
        onNavigateToCamera = onNavigateToCamera,
        onNavigateToOnboarding = onNavigateToOnboarding,
        onNavigateToRoute = onNavigateToRoute,
        onRefreshAi = viewModel::refreshAiSummary,
        modifier = modifier
    )
}

@Composable
fun HomeScreenContent(
    state: HomeUiState,
    onNavigateToSimulator: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onRefreshAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SOLAR ROBO",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = SolarAmber,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Dual-Axis Robotic Tracking Platform",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            StatusBadge(
                text = state.connectionText,
                isActive = state.isConnected,
                activeColor = SolarEmerald,
                inactiveColor = SolarRose
            )
        }

        // Live Telemetry Grid
        SolarCard {
            Text(
                text = "Live Telemetry & Diagnostics",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Mode",
                    value = state.mode.name,
                    valueColor = when (state.mode) {
                        RoboMode.NORMAL -> SolarEmerald
                        RoboMode.FAULT -> SolarRose
                        RoboMode.PROTECTING, RoboMode.SAFE -> SolarSky
                        else -> SolarAmber
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = SolarSky)
                    }
                )
                MetricItem(
                    label = "Panel Angle",
                    value = "${state.panelAngleDeg.toInt()}°",
                    unit = "Tilt",
                    valueColor = SolarAmber,
                    leadingIcon = {
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = SolarAmber)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Solar Generation",
                    value = "${state.generationWatts.toInt()}",
                    unit = "Watts",
                    valueColor = SolarEmerald,
                    leadingIcon = {
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = SolarEmerald)
                    }
                )
                MetricItem(
                    label = "Battery Reserve",
                    value = "${state.batteryPercent.toInt()}%",
                    unit = "SoC",
                    valueColor = if (state.batteryPercent > 20f) SolarSky else SolarRose,
                    leadingIcon = {
                        Icon(Icons.Default.BatteryFull, contentDescription = null, tint = SolarSky)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Environment",
                    value = state.environmentText,
                    valueColor = TextPrimary,
                    leadingIcon = {
                        Icon(Icons.Default.Thermostat, contentDescription = null, tint = SolarAmber)
                    }
                )
                MetricItem(
                    label = "Safety Gate",
                    value = state.safetyLevel.name,
                    valueColor = if (state.safetyLevel == SafetyLevel.NORMAL) SolarEmerald else SolarRose,
                    leadingIcon = {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = SolarEmerald)
                    }
                )
            }
        }

        // AI Assistant Speech Bubble
        SolarCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Robo Talk (AI Engine Summary)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SolarSky
                )
                IconButton(onClick = onRefreshAi) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh AI", tint = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "“${state.latestMessage}”",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }

        // Primary Navigation Action Buttons
        Text(
            text = "Active Features",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateToSimulator,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolarAmber,
                    contentColor = DarkBackground
                )
            ) {
                Text("Simulator", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNavigateToCamera,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolarSky,
                    contentColor = DarkBackground
                )
            ) {
                Text("Camera", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNavigateToOnboarding,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkBorder,
                    contentColor = TextPrimary
                )
            ) {
                Text("Onboarding", fontWeight = FontWeight.Bold)
            }
        }

        // Planned Architectural Modules
        Text(
            text = "Planned Feature Routes (Build Order)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted
        )

        val plannedModules = listOf(
            "energy" to "Energy Center",
            "control" to "Tracker Control",
            "safety" to "Safety & Emergency Gate",
            "environment" to "Environment Array",
            "activity" to "Activity Ledger",
            "health" to "Subsystem Diagnostics",
            "analytics" to "Yield Analytics",
            "notifications" to "Alert Notifications",
            "settings" to "Settings & DataStore"
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            plannedModules.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (route, title) ->
                        OutlinedButton(
                            onClick = { onNavigateToRoute(route) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(title, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    SolarRoboTheme {
        HomeScreenContent(
            state = HomeUiState(),
            onNavigateToSimulator = {},
            onNavigateToCamera = {},
            onNavigateToOnboarding = {},
            onNavigateToRoute = {},
            onRefreshAi = {}
        )
    }
}
