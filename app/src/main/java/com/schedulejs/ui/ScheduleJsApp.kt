package com.schedulejs.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.flow.map

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ScheduleJsApp() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModelFactory = remember(context.applicationContext) {
        ScheduleJsViewModel.factory(context.applicationContext)
    }
    val viewModel: ScheduleJsViewModel = viewModel(factory = viewModelFactory)
    val isReviewPendingToday by remember(viewModel) {
        viewModel.reviewState.map { it.isPendingToday }
    }.collectAsStateWithLifecycle(initialValue = false)
    val navController = rememberNavController()
    val screens = remember {
        listOf(
        AppScreen.Dashboard,
        AppScreen.Workout,
        AppScreen.Study,
        AppScreen.Review
        )
    }
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry.value?.destination
    val currentScreen = currentDestination
        ?.hierarchy
        ?.mapNotNull { destination -> allScreens.firstOrNull { it.route == destination.route } }
        ?.firstOrNull()
        ?: AppScreen.Dashboard

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUi()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(currentScreen.label) },
                actions = {
                    IconButton(
                        onClick = {
                            if (currentScreen.route != AppScreen.Settings.route) {
                                navController.navigate(AppScreen.Settings.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = AppScreen.Settings.label
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (screen == AppScreen.Review && isReviewPendingToday) {
                                        Badge()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selected) screen.filledIcon else screen.outlinedIcon,
                                    contentDescription = screen.label
                                )
                            }
                        },
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
                    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
                    DashboardScreen(dashboardState)
                }
                composable(AppScreen.Workout.route) {
                    val workoutState by viewModel.workoutState.collectAsStateWithLifecycle()
                    WorkoutScreen(
                        state = workoutState,
                        onBellyRoutineAction = viewModel::onBellyRoutineAction,
                        onCancelBellyRoutine = viewModel::cancelBellyRoutine,
                        onToggleWorkoutComplete = viewModel::toggleWorkoutComplete,
                        onSetChecked = viewModel::checkWorkoutSet,
                        onBellyRoutineRepTap = viewModel::onBellyRoutineRepTap
                    )
                }
                composable(AppScreen.Study.route) {
                    val studyState by viewModel.studyState.collectAsStateWithLifecycle()
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
                    val reviewStateForScreen by viewModel.reviewState.collectAsStateWithLifecycle()
                    ReviewScreen(
                        state = reviewStateForScreen,
                        onAnswerChange = viewModel::updateReviewAnswer,
                        onSave = viewModel::saveReview
                    )
                }
                composable(AppScreen.Settings.route) {
                    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
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

private val allScreens = listOf(
    AppScreen.Dashboard,
    AppScreen.Workout,
    AppScreen.Study,
    AppScreen.Review,
    AppScreen.Settings
)
