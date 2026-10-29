package com.solarrobo.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solarrobo.app.navigation.AppNavHost
import com.solarrobo.app.navigation.Screen
import com.solarrobo.core.ui.theme.LocalAccessMode
import com.solarrobo.core.ui.theme.SolarRoboTheme
import com.solarrobo.feature.auth.presentation.AuthViewModel
import com.solarrobo.feature.auth.presentation.CreateProfileScreen
import com.solarrobo.feature.auth.presentation.LockScreen
import com.solarrobo.feature.settings.presentation.SettingsViewModel

@Composable
fun SolarRoboApp() {
    val auth: AuthViewModel = hiltViewModel()
    val authState by auth.uiState.collectAsStateWithLifecycle()
    val accessMode by auth.accessMode.collectAsStateWithLifecycle()
    val isLocked by auth.isLocked.collectAsStateWithLifecycle()
    SolarRoboTheme {
        CompositionLocalProvider(LocalAccessMode provides accessMode) {
            when {
                authState.isLoading -> Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                authState.profile == null -> CreateProfileScreen(auth)
                isLocked -> LockScreen(auth)
                else -> MainSolarRoboContent()
            }
        }
    }
}

@Composable
private fun MainSolarRoboContent() {
    val navController = rememberNavController()
    hiltViewModel<SettingsViewModel>()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val destinations = listOf(
        Triple(Screen.Home, "Today", Icons.Default.Home),
        Triple(Screen.Control, "Track", Icons.Default.TrackChanges),
        Triple(Screen.Analytics, "Insights", Icons.Default.Insights),
        Triple(Screen.Settings, "Settings", Icons.Default.Settings)
    )
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
            modifier = Modifier.fillMaxSize().padding(padding),
            startDestination = Screen.Onboarding.route
        )
    }
}
