package com.schedulejs.ui

sealed class AppScreen(
    val route: String,
    val label: String,
    val shortLabel: String
) {
    data object Dashboard : AppScreen("dashboard", "Dashboard", "HUD")
    data object Workout : AppScreen("workout", "Workout", "Fit")
    data object Study : AppScreen("study", "Study", "Deep")
    data object Review : AppScreen("review", "Review", "Fri")
    data object Settings : AppScreen("settings", "Settings", "Cfg")
}
