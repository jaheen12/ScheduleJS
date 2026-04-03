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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.schedulejs.ui.viewmodel.ScheduleJsViewModel

@Composable
fun ScheduleJsApp() {
    val context = LocalContext.current
    val viewModel: ScheduleJsViewModel = viewModel(factory = ScheduleJsViewModel.factory(context.applicationContext))
    val dashboardState by viewModel.dashboardState.collectAsState()
    val workoutState by viewModel.workoutState.collectAsState()
    val studyState by viewModel.studyState.collectAsState()
    val reviewState by viewModel.reviewState.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
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
                    DashboardScreen(dashboardState)
                }
                composable(AppScreen.Workout.route) {
                    WorkoutScreen(
                        state = workoutState,
                        onBellyRoutineAction = viewModel::onBellyRoutineAction,
                        onCancelBellyRoutine = viewModel::cancelBellyRoutine,
                        onToggleWorkoutComplete = viewModel::toggleWorkoutComplete
                    )
                }
                composable(AppScreen.Study.route) {
                    StudyScreen(
                        state = studyState,
                        onFocusTimerAction = viewModel::onFocusTimerAction,
                        onCancelFocusTimer = viewModel::cancelFocusTimer,
                        onToggleFocusMode = viewModel::toggleFocusMode,
                        onRequestDndPermission = {
                            context.startActivity(viewModel.buildDndPermissionIntent())
                        }
                    )
                }
                composable(AppScreen.Review.route) {
                    ReviewScreen(
                        state = reviewState,
                        onAnswerChange = viewModel::updateReviewAnswer,
                        onSave = viewModel::saveReview
                    )
                }
                composable(AppScreen.Settings.route) {
                    SettingsScreen(
                        state = settingsState,
                        onLeadTimeSelected = viewModel::updateNotificationLeadTime,
                        onTransitAlertsChanged = viewModel::updateTransitAlerts,
                        onWakeUpTimeChanged = viewModel::updateTemplateWakeUpTime,
                        onTaskFieldChanged = viewModel::updateTaskField,
                        onSave = viewModel::saveSettings,
                        onPermissionAction = { cardId ->
                            val intent = when (cardId) {
                                "notifications" -> viewModel.buildNotificationPermissionIntent()
                                "exact_alarms" -> viewModel.buildExactAlarmPermissionIntent()
                                "dnd" -> viewModel.buildDndPermissionIntent()
                                else -> viewModel.buildBatteryOptimizationIntent()
                            }
                            context.startActivity(intent)
                        },
                        onPermissionDismiss = viewModel::dismissPermissionEducation
                    )
                }
            }
        }
    }
}
