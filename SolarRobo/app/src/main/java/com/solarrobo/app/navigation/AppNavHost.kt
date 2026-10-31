package com.solarrobo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.solarrobo.app.presentation.HomeScreen
import com.solarrobo.app.presentation.InsightsScreen
import com.solarrobo.app.presentation.TrackScreen
import com.solarrobo.app.presentation.WeatherViewModel
import com.solarrobo.feature.auth.presentation.AuthViewModel
import com.solarrobo.feature.auth.presentation.SecuritySettingsScreen
import com.solarrobo.feature.camera.presentation.CameraScreen
import com.solarrobo.feature.notifications.presentation.NotificationsScreen
import com.solarrobo.feature.onboarding.presentation.OnboardingScreen
import com.solarrobo.feature.settings.presentation.SettingsScreen
import com.solarrobo.feature.simulator.presentation.SimulatorScreen
import com.solarrobo.feature.talk.presentation.TalkScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("today")
    data object Control : Screen("track")
    data object Analytics : Screen("insights")
    data object Settings : Screen("settings")
    data object Security : Screen("security")
    data object Camera : Screen("camera")
    data object Notifications : Screen("notifications")
    data object Simulator : Screen("simulator")
    data object Talk : Screen("talk")
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = hiltViewModel(),
                weatherViewModel = hiltViewModel<WeatherViewModel>(),
                onNavigateToSimulator = { navController.navigate(Screen.Simulator.route) },
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToOnboarding = { navController.navigate(Screen.Onboarding.route) },
                onNavigateToRoute = { route -> navController.navigate(route) }
            )
        }
        composable(Screen.Control.route) {
            TrackScreen(viewModel = hiltViewModel(), onSetup = { navController.navigate(Screen.Onboarding.route) })
        }
        composable(Screen.Analytics.route) {
            InsightsScreen(viewModel = hiltViewModel(), weatherViewModel = hiltViewModel())
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = hiltViewModel(), onNavigateToSecurity = { navController.navigate(Screen.Security.route) })
        }
        composable(Screen.Security.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            SecuritySettingsScreen(authViewModel, onLockNow = authViewModel::lockNow)
        }
        composable(Screen.Camera.route) { CameraScreen(viewModel = hiltViewModel()) }
        composable(Screen.Notifications.route) { NotificationsScreen(viewModel = hiltViewModel()) }
        composable(Screen.Simulator.route) { SimulatorScreen(viewModel = hiltViewModel()) }
        composable(Screen.Talk.route) { TalkScreen(viewModel = hiltViewModel()) }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(viewModel = hiltViewModel(), onOnboardingFinished = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
    }
}
