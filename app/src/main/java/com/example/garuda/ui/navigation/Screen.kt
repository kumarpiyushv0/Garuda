package com.example.garuda.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Sos : Screen("sos")
    object Map : Screen("map")
    object Contacts : Screen("contacts")
    object Settings : Screen("settings")
}
