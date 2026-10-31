package com.solarrobo.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.CircularProgressIndicator
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
import com.solarrobo.core.ui.components.AstraBottomNavigation
import com.solarrobo.core.ui.components.AstraDestination
import com.solarrobo.core.ui.components.AstraTopBar
import com.solarrobo.core.ui.theme.LocalAccessMode
import com.solarrobo.core.ui.theme.SolarRoboTheme
import com.solarrobo.feature.auth.presentation.AuthViewModel
import com.solarrobo.feature.auth.presentation.CreateProfileScreen
import com.solarrobo.feature.auth.presentation.LockScreen
import com.solarrobo.feature.settings.presentation.SettingsViewModel
import com.solarrobo.app.presentation.TrackViewModel

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
    val trackerViewModel: TrackViewModel = hiltViewModel()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val destinations = listOf(
        AstraDestination(Screen.Home.route, "Home", Icons.Default.Home),
        AstraDestination(Screen.Control.route, "Track", Icons.Default.TrackChanges),
        AstraDestination(Screen.Analytics.route, "Sky", Icons.Default.Cloud),
        AstraDestination(Screen.Analytics.route, "History", Icons.Default.History),
        AstraDestination(Screen.Settings.route, "More", Icons.Default.Settings)
    )
    val showNavigation = currentRoute != Screen.Onboarding.route
    val title = when (currentRoute) {
        Screen.Home.route -> "ASTRA"
        Screen.Control.route -> "Track"
        Screen.Analytics.route -> "Sky & History"
        Screen.Settings.route -> "More"
        else -> "ASTRA"
    }
    Scaffold(
        topBar = {
            if (showNavigation) AstraTopBar(title = title, onStop = trackerViewModel::emergencyStop)
        },
        bottomBar = {
            if (showNavigation) {
                AstraBottomNavigation(
                    currentRoute = currentRoute,
                    destinations = destinations.distinctBy { it.label },
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
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
