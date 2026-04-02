package com.schedulejs.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.schedulejs.ui.screens.DashboardScreen
import com.schedulejs.ui.screens.ReviewScreen
import com.schedulejs.ui.screens.SettingsScreen
import com.schedulejs.ui.screens.StudyScreen
import com.schedulejs.ui.screens.WorkoutScreen

@Composable
fun ScheduleJsApp() {
    val navController = rememberNavController()
    val screens = listOf(
        AppScreen.Dashboard,
        AppScreen.Workout,
        AppScreen.Study,
        AppScreen.Review,
        AppScreen.Settings
    )
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry.value?.destination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(screen.shortLabel) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = AppScreen.Dashboard.route
            ) {
                composable(AppScreen.Dashboard.route) {
                    DashboardScreen(DemoData.dashboard)
                }
                composable(AppScreen.Workout.route) {
                    WorkoutScreen(DemoData.workout)
                }
                composable(AppScreen.Study.route) {
                    StudyScreen(DemoData.study)
                }
                composable(AppScreen.Review.route) {
                    ReviewScreen(DemoData.review)
                }
                composable(AppScreen.Settings.route) {
                    SettingsScreen(DemoData.settings)
                }
            }
        }
    }
}
