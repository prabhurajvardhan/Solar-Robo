package com.solarrobo.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solarrobo.app.navigation.AppNavHost
import com.solarrobo.app.navigation.Screen
import com.solarrobo.core.ui.theme.SolarRoboTheme
import com.solarrobo.feature.settings.presentation.SettingsViewModel

@Composable
fun SolarRoboApp() {
    val navController = rememberNavController()
    hiltViewModel<SettingsViewModel>()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val destinations = listOf(
        Triple(Screen.Home, "Today", Icons.Default.Home),
        Triple(Screen.Control, "Track", Icons.Default.TrackChanges),
        Triple(Screen.Analytics, "Insights", Icons.Default.Insights),
        Triple(Screen.Settings, "Settings", Icons.Default.Settings)
    )
    SolarRoboTheme {
        val showNavigation = currentRoute != Screen.Onboarding.route
        Scaffold(
            bottomBar = {
                if (showNavigation) {
                    NavigationBar {
                        destinations.forEach { (screen, label, icon) ->
                            NavigationBarItem(
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { androidx.compose.material3.Icon(icon, contentDescription = label) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(padding),
                startDestination = Screen.Onboarding.route
            )
        }
    }
}
