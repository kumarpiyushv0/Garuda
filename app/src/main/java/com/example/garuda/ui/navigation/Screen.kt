package com.example.garuda.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Sos : Screen("sos")
    object Map : Screen("map")
    object Contacts : Screen("contacts")
    object Login : Screen("login")
    object Settings : Screen("settings")
    object Recordings : Screen("recordings")
}
