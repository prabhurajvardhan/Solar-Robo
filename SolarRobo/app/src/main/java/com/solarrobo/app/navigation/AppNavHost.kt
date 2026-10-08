package com.solarrobo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.solarrobo.feature.camera.presentation.CameraScreen
import com.solarrobo.feature.energy.presentation.EnergyScreen
import com.solarrobo.feature.home.presentation.HomeScreen
import com.solarrobo.feature.notifications.presentation.NotificationsScreen
import com.solarrobo.feature.onboarding.presentation.OnboardingScreen
import com.solarrobo.feature.settings.presentation.SettingsScreen
import com.solarrobo.feature.simulator.presentation.SimulatorScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Energy : Screen("energy")
    data object Control : Screen("control")
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
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = hiltViewModel(),
                onNavigateToEnergy = { navController.navigate(Screen.Energy.route) },
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToSimulator = { navController.navigate(Screen.Simulator.route) },
                onNavigateToSafety = { navController.navigate(Screen.Notifications.route) }
            )
        }
        composable(Screen.Energy.route) {
            EnergyScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Camera.route) {
            CameraScreen(viewModel = hiltViewModel())
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(viewModel = hiltViewModel())
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = hiltViewModel())
        }
        composable(Screen.Simulator.route) {
            SimulatorScreen(viewModel = hiltViewModel())
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                viewModel = hiltViewModel(),
                onOnboardingFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
    }
}