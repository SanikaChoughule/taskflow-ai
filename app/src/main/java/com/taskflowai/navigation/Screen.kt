package com.taskflowai.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Settings : Screen("settings")

    // Legacy compatibility aliases redirected to the 3 core screens
    data object Dashboard : Screen("home")
    data object Command : Screen("home")
    data object Calendar : Screen("home")
    data object TaskPlan : Screen("home")
    data object Execution : Screen("home")
    data object TaskDetails : Screen("home") {
        fun createRoute(taskId: String) = "home"
    }
    data object HistoryDetails : Screen("history") {
        fun createRoute(taskId: String) = "history"
    }
    data object Reminders : Screen("home")
    data object Suggestions : Screen("home")
    data object Profile : Screen("settings")
    data object About : Screen("settings")
    data object Splash : Screen("home")
    data object Onboarding : Screen("home")
    data object Login : Screen("login")
}
