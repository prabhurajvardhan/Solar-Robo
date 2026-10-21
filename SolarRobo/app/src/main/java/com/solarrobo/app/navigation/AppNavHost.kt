package com.solarrobo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.solarrobo.app.presentation.OperationsScreen
import com.solarrobo.app.presentation.ModelLibraryScreen
import com.solarrobo.feature.camera.presentation.CameraScreen
import com.solarrobo.feature.energy.presentation.EnergyScreen
import com.solarrobo.feature.home.presentation.HomeScreen
import com.solarrobo.feature.notifications.presentation.NotificationsScreen
import com.solarrobo.feature.onboarding.presentation.OnboardingScreen
import com.solarrobo.feature.settings.presentation.SettingsScreen
import com.solarrobo.feature.simulator.presentation.SimulatorScreen
import com.solarrobo.feature.talk.presentation.TalkScreen

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
    data object Talk : Screen("talk")
    data object Models : Screen("models")
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
                onNavigateToControl = { navController.navigate(Screen.Control.route) },
                onNavigateToSafety = { navController.navigate(Screen.Safety.route) },
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToSimulator = { navController.navigate(Screen.Simulator.route) }
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
        composable(Screen.Talk.route) {
            TalkScreen(viewModel = hiltViewModel())
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
        composable(Screen.Control.route) {
            OperationsScreen("Tracker Control", "control", { navController.popBackStack() })
        }
        composable(Screen.Safety.route) {
            OperationsScreen("Safety & Emergency", "safety", { navController.popBackStack() })
        }
        composable(Screen.Environment.route) {
            OperationsScreen("Environment Array", "environment", { navController.popBackStack() })
        }
        composable(Screen.Activity.route) {
            OperationsScreen("Activity Ledger", "activity", { navController.popBackStack() })
        }
        composable(Screen.Health.route) {
            OperationsScreen("Subsystem Diagnostics", "health", { navController.popBackStack() })
        }
        composable(Screen.Analytics.route) {
            OperationsScreen("Yield Analytics", "analytics", { navController.popBackStack() })
        }
        composable(Screen.Models.route) {
            ModelLibraryScreen(onBack = { navController.popBackStack() })
        }
    }
}
