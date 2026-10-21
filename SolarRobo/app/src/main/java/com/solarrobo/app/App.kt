package com.solarrobo.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solarrobo.app.navigation.AppNavHost
import com.solarrobo.app.navigation.Screen
import com.solarrobo.app.presentation.MascotOverlay
import com.solarrobo.core.ui.theme.ForestInk
import com.solarrobo.core.ui.theme.Porcelain
import com.solarrobo.core.ui.theme.SolarCitron
import com.solarrobo.core.ui.theme.SolarRoboTheme
import com.solarrobo.feature.settings.presentation.SettingsViewModel

@Composable
fun SolarRoboApp() {
    val navController = rememberNavController()
    hiltViewModel<SettingsViewModel>()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var menuOpen by remember { mutableStateOf(false) }
    val primary = listOf(
        Screen.Home to ("Home" to Icons.Default.Home),
        Screen.Energy to ("Energy" to Icons.Default.BatteryChargingFull),
        Screen.Camera to ("Camera" to Icons.Default.PhotoCamera),
        Screen.Notifications to ("Alerts" to Icons.Default.Notifications),
        Screen.Talk to ("Talk" to Icons.Default.ChatBubble),
        Screen.Models to ("Models" to Icons.Default.Memory),
        Screen.Settings to ("Settings" to Icons.Default.Settings),
    )
    SolarRoboTheme(darkTheme = false) {
        Box(Modifier.fillMaxSize()) {
            AppNavHost(navController, Modifier.fillMaxSize(), startDestination = Screen.Onboarding.route)
            if (currentRoute != Screen.Onboarding.route) {
                MascotOverlay(currentRoute, Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 22.dp))
                Box(Modifier.fillMaxSize().padding(end = 20.dp, bottom = 22.dp), contentAlignment = Alignment.BottomEnd) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (menuOpen) {
                            primary.asReversed().forEach { (screen, labelIcon) ->
                                val (label, icon) = labelIcon
                                Surface(color = Porcelain, shadowElevation = 8.dp, shape = MaterialTheme.shapes.large, onClick = {
                                    menuOpen = false
                                    navController.navigate(screen.route) { launchSingleTop = true }
                                }) {
                                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(label, color = ForestInk, style = MaterialTheme.typography.labelLarge)
                                        Spacer(Modifier.width(10.dp))
                                        Icon(icon, label, tint = ForestInk)
                                    }
                                }
                            }
                        }
                        FloatingActionButton(onClick = { menuOpen = !menuOpen }, containerColor = if (menuOpen) ForestInk else SolarCitron, contentColor = if (menuOpen) Porcelain else ForestInk) {
                            Icon(if (menuOpen) Icons.Default.Close else Icons.Default.Menu, "Open navigation")
                        }
                    }
                }
            }
        }
    }
}
