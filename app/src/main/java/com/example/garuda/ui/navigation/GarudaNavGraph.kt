package com.example.garuda.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun GarudaNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Onboarding.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            com.example.garuda.ui.screens.OnboardingScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            com.example.garuda.ui.screens.LoginScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            com.example.garuda.ui.screens.HomeScreen(navController = navController)
        }
        composable(Screen.Sos.route) {
            com.example.garuda.ui.screens.SosScreen(navController = navController)
        }
        composable(Screen.Map.route) {
            com.example.garuda.ui.screens.MapScreen(navController = navController)
        }
        composable(Screen.Contacts.route) {
            com.example.garuda.ui.screens.ContactsScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            Text("Settings Screen")
        }
        composable(Screen.Recordings.route) {
            com.example.garuda.ui.screens.RecordingsScreen(navController = navController)
        }
    }
}
