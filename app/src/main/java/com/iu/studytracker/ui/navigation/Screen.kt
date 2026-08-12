package com.iu.studytracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
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
    object Setup : Screen("setup", "Monthly Setup")
    object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    object Progress : Screen("progress", "Progress", Icons.AutoMirrored.Filled.TrendingUp)
    object Roadmap : Screen("roadmap", "Roadmap", Icons.Default.Map)
    object Curriculum : Screen("curriculum", "Curriculum", Icons.Default.School)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        /** Bottom navigation items (excludes Setup which is a full-screen flow). */
        val bottomNavItems = listOf(Dashboard, Calendar, Progress, Roadmap, Curriculum)
    }
}
