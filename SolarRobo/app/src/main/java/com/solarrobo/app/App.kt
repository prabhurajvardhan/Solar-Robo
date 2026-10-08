package com.solarrobo.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solarrobo.app.navigation.AppNavHost
import com.solarrobo.app.navigation.Screen
import com.solarrobo.feature.settings.presentation.SettingsViewModel

@Composable
fun SolarRoboApp() {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.uiState.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val colorScheme = if (settings.settings?.darkTheme != false) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    listOf(
                        Screen.Home to "Home",
                        Screen.Energy to "Energy",
                        Screen.Camera to "Camera",
                        Screen.Notifications to "Alerts",
                        Screen.Settings to "Settings"
                    ).forEach { (screen, label) ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    launchSingleTop = true
                                }
                            },
                            icon = {},
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            AppNavHost(navController, Modifier.padding(innerPadding))
        }
    }
}
