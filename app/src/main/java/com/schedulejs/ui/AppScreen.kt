package com.schedulejs.ui

import androidx.compose.material.icons.filled.Home as HomeFilled
import androidx.compose.material.icons.filled.FitnessCenter as FitnessCenterFilled
import androidx.compose.material.icons.filled.MenuBook as MenuBookFilled
import androidx.compose.material.icons.filled.RateReview as RateReviewFilled
import androidx.compose.material.icons.outlined.Home as HomeOutlined
import androidx.compose.material.icons.outlined.MenuBook as MenuBookOutlined
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppScreen(
    val route: String,
    val label: String
) {
    sealed class TopLevelScreen(
        route: String,
        label: String,
        val filledIcon: ImageVector,
        val outlinedIcon: ImageVector
    ) : AppScreen(route, label)

    data object Dashboard : TopLevelScreen(
        route = "dashboard",
        label = "Dashboard",
        filledIcon = HomeFilled,
        outlinedIcon = HomeOutlined
    )

    data object Workout : TopLevelScreen(
        route = "workout",
        label = "Workout",
        filledIcon = FitnessCenterFilled,
        outlinedIcon = FitnessCenterFilled
    )

    data object Study : TopLevelScreen(
        route = "study",
        label = "Study",
        filledIcon = MenuBookFilled,
        outlinedIcon = MenuBookOutlined
    )

    data object Review : TopLevelScreen(
        route = "review",
        label = "Review",
        filledIcon = RateReviewFilled,
        outlinedIcon = RateReviewFilled
    )

    data object Settings : AppScreen("settings", "Settings")
}
