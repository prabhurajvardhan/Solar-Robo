package com.solarrobo.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.solarrobo.app.navigation.AppNavHost

@Composable
fun SolarRoboApp() {
    val navController = rememberNavController()
    MaterialTheme {
        Scaffold { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
