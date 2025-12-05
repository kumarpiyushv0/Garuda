package com.example.garuda.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun GarudaNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.route) {
            com.example.garuda.ui.screens.OnboardingScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            com.example.garuda.ui.screens.HomeScreen(navController = navController)
        }
        composable(Screen.Sos.route) {
            com.example.garuda.ui.screens.SosScreen(navController = navController)
        }
        composable(Screen.Map.route) {
            Text("Map Screen")
        }
        composable(Screen.Contacts.route) {
            Text("Contacts Screen")
        }
        composable(Screen.Settings.route) {
            Text("Settings Screen")
        }
    }
}
