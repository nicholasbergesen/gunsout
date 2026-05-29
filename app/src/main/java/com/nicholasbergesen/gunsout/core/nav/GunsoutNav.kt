package com.nicholasbergesen.gunsout.core.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nicholasbergesen.gunsout.feature.body.BodyScreen
import com.nicholasbergesen.gunsout.feature.diet.DietScreen
import com.nicholasbergesen.gunsout.feature.history.HistoryDetailScreen
import com.nicholasbergesen.gunsout.feature.history.HistoryListScreen
import com.nicholasbergesen.gunsout.feature.library.ExerciseEditScreen
import com.nicholasbergesen.gunsout.feature.library.LibraryListScreen
import com.nicholasbergesen.gunsout.feature.program.ProgramEditScreen
import com.nicholasbergesen.gunsout.feature.program.ProgramListScreen
import com.nicholasbergesen.gunsout.feature.settings.SettingsScreen
import com.nicholasbergesen.gunsout.feature.today.TodayScreen
import com.nicholasbergesen.gunsout.feature.workout.SessionScreen

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    NavTab(Routes.TODAY, "Today", Icons.Filled.Today),
    NavTab(Routes.PROGRAMS, "Programs", Icons.Filled.FitnessCenter),
    NavTab(Routes.DIET, "Diet", Icons.Filled.Restaurant),
    NavTab(Routes.BODY, "Body", Icons.Filled.MonitorWeight),
    NavTab(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

@Composable
fun GunsoutApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            val isTopLevel = currentRoute in tabs.map { it.route }
            if (isTopLevel) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(inner)
        ) {
            composable(Routes.TODAY) {
                TodayScreen(
                    onStartSession = { id -> navController.navigate(Routes.session(id)) },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) }
                )
            }
            composable(
                Routes.SESSION,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("sessionId") ?: 0L
                SessionScreen(sessionId = id, onFinished = { navController.popBackStack() })
            }

            composable(Routes.PROGRAMS) {
                ProgramListScreen(onEdit = { id -> navController.navigate(Routes.program(id)) })
            }
            composable(
                Routes.PROGRAM_EDIT,
                arguments = listOf(navArgument("programId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("programId") ?: 0L
                ProgramEditScreen(programId = id, onBack = { navController.popBackStack() })
            }

            composable(Routes.HISTORY) {
                HistoryListScreen(onOpenSession = { id -> navController.navigate(Routes.history(id)) })
            }
            composable(
                Routes.HISTORY_DETAIL,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("sessionId") ?: 0L
                HistoryDetailScreen(sessionId = id, onBack = { navController.popBackStack() })
            }

            composable(Routes.DIET) {
                DietScreen()
            }

            composable(Routes.LIBRARY) {
                LibraryListScreen(
                    onEdit = { id -> navController.navigate(Routes.exercise(id)) },
                    onCreate = { navController.navigate(Routes.exercise(0L)) }
                )
            }
            composable(
                Routes.LIBRARY_EDIT,
                arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("exerciseId") ?: 0L
                ExerciseEditScreen(exerciseId = id, onBack = { navController.popBackStack() })
            }

            composable(Routes.BODY) { BodyScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenLibrary = { navController.navigate(Routes.LIBRARY) }
                )
            }
        }
    }
}
