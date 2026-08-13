package com.iu.studytracker.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iu.studytracker.ui.screen.calendar.CalendarScreen
import com.iu.studytracker.ui.screen.dashboard.DashboardScreen
import com.iu.studytracker.ui.screen.progress.ProgressScreen
import com.iu.studytracker.ui.screen.studynow.StudyNowScreen
import com.iu.studytracker.ui.screen.setup.SetupScreen
import com.iu.studytracker.ui.screen.curriculum.CurriculumScreen
import com.iu.studytracker.ui.theme.Purple60

@Composable
fun StudyTrackerNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide navigation bar/rail on Setup screen
    val showNav = currentDestination?.route != Screen.Setup.route

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet && showNav) {
            // ── Tablet / Large Screen: Navigation Rail (Side) ────────
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = Purple60,
                    modifier = Modifier.fillMaxHeight(),
                    header = {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true

                        NavigationRailItem(
                            icon = {
                                screen.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
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
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Purple60,
                                selectedTextColor = Purple60,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AppNavHost(navController = navController)
                }
            }
        } else {
            // ── Phone / Compact Screen: Bottom Navigation Bar ────────
            Scaffold(
                bottomBar = {
                    if (showNav) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            Screen.bottomNavItems.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == screen.route
                                } == true

                                NavigationBarItem(
                                    icon = {
                                        screen.icon?.let {
                                            Icon(
                                                imageVector = it,
                                                contentDescription = screen.title
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
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
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Purple60,
                                        selectedTextColor = Purple60,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AppNavHost(navController = navController)
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToSetup = {
                    navController.navigate(Screen.Setup.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            Screen.Setup.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(400)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(400)
                )
            }
        ) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Progress.route) {
            ProgressScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.StudyNow.route) {
            StudyNowScreen()
        }

        composable(Screen.Curriculum.route) {
            CurriculumScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Settings.route) {
            com.iu.studytracker.ui.screen.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

