package com.iu.studytracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for the app.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Login : Screen("login", "Login")
    object Setup : Screen("setup", "Monthly Setup")
    object ManualSchedule : Screen("manual_schedule/{moduleIds}", "Plan My Week") {
        fun createRoute(moduleIds: List<String>) = "manual_schedule/${moduleIds.joinToString(",")}"
    }
    object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    object Analytics : Screen("analytics", "Analytics", Icons.AutoMirrored.Filled.TrendingUp)
    object StudyNow : Screen("study_now", "Study", Icons.Default.Timer)
    object Curriculum : Screen("curriculum", "Curriculum", Icons.Default.School)
    object ModuleDetails : Screen("module_details/{moduleId}", "Module Details") {
        fun createRoute(moduleId: String) = "module_details/$moduleId"
    }
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Templates : Screen("templates", "Templates")
    object FocusMode : Screen("focus_mode", "Focus Mode")

    object Matrix : Screen("matrix", "Matrix", Icons.Default.GridView)

    companion object {
        val bottomNavItems = listOf(Dashboard, Matrix, StudyNow, Calendar, Curriculum, Analytics)
    }
}
