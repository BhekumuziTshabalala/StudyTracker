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
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iu.studytracker.ui.screen.calendar.CalendarScreen
import com.iu.studytracker.ui.screen.dashboard.DashboardScreen
import com.iu.studytracker.ui.screen.studynow.StudyNowScreen
import com.iu.studytracker.ui.screen.setup.SetupScreen
import com.iu.studytracker.ui.screen.curriculum.CurriculumScreen
import com.iu.studytracker.ui.theme.OceanBlue

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
                    contentColor = OceanBlue,
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
                            // label removed for icon-only design
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
                                selectedIconColor = OceanBlue,
                                selectedTextColor = OceanBlue,
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
                                    // label removed for icon-only design
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
                                        selectedIconColor = OceanBlue,
                                        selectedTextColor = OceanBlue,
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
            fadeIn(animationSpec = tween(400)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(400),
                initialOffset = { it / 8 }
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(400)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(400),
                targetOffset = { -it / 8 }
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(400)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(400),
                initialOffset = { -it / 8 }
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(400)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(400),
                targetOffset = { it / 8 }
            )
        }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToSetup = {
                    navController.navigate(Screen.Setup.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToTemplates = {
                    navController.navigate(Screen.Templates.route)
                },
                onNavigateToFocusMode = {
                    navController.navigate(Screen.FocusMode.route)
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

        composable(Screen.Analytics.route) {
            com.iu.studytracker.ui.screen.analytics.AnalyticsScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Matrix.route) {
            com.iu.studytracker.ui.screen.matrix.EisenhowerMatrixScreen()
        }

        composable(Screen.StudyNow.route) {
            StudyNowScreen()
        }

        composable(Screen.Curriculum.route) {
            CurriculumScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToModuleDetails = { moduleId ->
                    navController.navigate(Screen.ModuleDetails.createRoute(moduleId))
                }
            )
        }

        composable(
            route = Screen.ModuleDetails.route,
            arguments = listOf(navArgument("moduleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId") ?: return@composable
            com.iu.studytracker.ui.screen.curriculum.details.ModuleDetailsScreen(
                moduleId = moduleId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            com.iu.studytracker.ui.screen.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Templates.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val repository = (context.applicationContext as com.iu.studytracker.StudyTrackerApp).repository
            val viewModel: com.iu.studytracker.ui.screen.templates.TaskTemplatesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.iu.studytracker.ui.screen.templates.TaskTemplatesViewModelFactory(repository)
            )
            com.iu.studytracker.ui.screen.templates.TaskTemplatesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FocusMode.route) {
            com.iu.studytracker.ui.screen.focus.FocusModeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

