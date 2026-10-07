package com.solarrobo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.solarrobo.feature.notifications.presentation.NotificationsScreen

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
        composable(Screen.Notifications.route) {
            NotificationsScreen(viewModel = hiltViewModel())
        }
    }
}
