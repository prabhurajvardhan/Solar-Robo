package com.solarrobo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.solarrobo.feature.camera.presentation.CameraScreen
import com.solarrobo.feature.notifications.presentation.NotificationsScreen
import com.solarrobo.feature.settings.presentation.SettingsScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Energy : Screen("energy")
    data object Control : Screen("control")
    data object Talk : Screen("talk")
    data object Safety : Screen("safety")
    data object Camera : Screen("camera")
    data object Environment : Screen("environment")
    data object Activity : Screen("activity")
    data object Health : Screen("health")
    data object Analytics : Screen("analytics")
    data object Notifications : Screen("notifications")
    data object Settings : Screen("settings")
    data object Simulator : Screen("simulator")
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Notifications.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Camera.route) {
            CameraScreen(viewModel = hiltViewModel())
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(viewModel = hiltViewModel())
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = hiltViewModel())
        }
    }
}