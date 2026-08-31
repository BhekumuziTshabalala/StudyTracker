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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iu.studytracker.ui.screen.calendar.CalendarScreen
import com.iu.studytracker.ui.screen.dashboard.DashboardScreen
import com.iu.studytracker.ui.screen.studynow.StudyNowScreen
import com.iu.studytracker.ui.screen.setup.SetupScreen
import com.iu.studytracker.ui.screen.curriculum.CurriculumScreen
import com.iu.studytracker.ui.theme.OceanBlue
import com.iu.studytracker.ui.screen.studynow.StudyNowScreen
import kotlinx.coroutines.launch
import com.iu.studytracker.ui.screen.login.LoginScreen

@Composable
fun StudyTrackerNavGraph(startDestination: String = Screen.Dashboard.route) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide navigation bar/rail on Setup, ManualSchedule, and Login screens
    val showNav = currentDestination?.route != Screen.Setup.route && 
                  currentDestination?.route?.startsWith("manual_schedule") != true &&
                  currentDestination?.route != Screen.Login.route

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
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                ) {
                    Spacer(modifier = Modifier.weight(1f))
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
                            label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
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
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AppNavHost(navController = navController, startDestination = startDestination)
                }
            }
        } else {
            // ── Phone / Compact Screen: Bottom Navigation Bar ────────
            Scaffold(
                bottomBar = {
                    if (showNav) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
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
                                    label = null,
                                    alwaysShowLabel = false,
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
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    AppNavHost(navController = navController, startDestination = startDestination)
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    startDestination: String = Screen.Dashboard.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(350)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350),
                initialOffset = { it / 10 }
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(350)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350),
                targetOffset = { -it / 10 }
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(350)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350),
                initialOffset = { -it / 10 }
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(350)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350),
                targetOffset = { it / 10 }
            )
        }
    ) {
        composable(Screen.Login.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            LoginScreen(
                onLoginSuccess = {
                    val app = context.applicationContext as com.iu.studytracker.StudyTrackerApp
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        app.syncManager.initialize()
                    }
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
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
                },
                onNavigateToManualSchedule = { moduleIds ->
                    navController.navigate(Screen.ManualSchedule.createRoute(moduleIds))
                }
            )
        }

        composable(
            route = Screen.ManualSchedule.route,
            arguments = listOf(navArgument("moduleIds") { type = NavType.StringType })
        ) { backStackEntry ->
            val moduleIdsString = backStackEntry.arguments?.getString("moduleIds") ?: ""
            val moduleIds = moduleIdsString.split(",").filter { it.isNotBlank() }
            com.iu.studytracker.ui.screen.schedule.ManualScheduleScreen(
                moduleIds = moduleIds,
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
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
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
