package com.msafonov.swimresults.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.msafonov.swimresults.ui.navigation.Screen
import com.msafonov.swimresults.ui.navigation.bottomNavItems
import com.msafonov.swimresults.ui.theme.SwimRankAppTheme
import com.msafonov.swimresults.ui.theme.Theme
import com.msafonov.swimresults.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SwimResultsApp() {
    val navController = rememberNavController()
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val isDark by settingsViewModel.isDark.collectAsStateWithLifecycle()
    SwimRankAppTheme(isDark) {
        Scaffold(
            containerColor = Theme.colors.backGround,
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                if (currentRoute != Screen.AddSwimResult.route) {
                NavigationBar(
                    containerColor = Theme.colors.backGround,
                ) {
                    bottomNavItems.forEach { navItem ->
                        NavigationBarItem(
                            alwaysShowLabel = false,
                            icon = {
                                Icon(
                                    imageVector = navItem.icon,
                                    contentDescription = stringResource(navItem.title)
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Theme.colors.textContrast,
                                unselectedIconColor = Theme.colors.textSecondary,
                                selectedTextColor = Theme.colors.textPrimary,
                                unselectedTextColor = Theme.colors.textSecondary,
                                indicatorColor = Theme.colors.primary
                            ),
                            label = {
                                Text(
                                    text = stringResource(navItem.title)
                                )
                            },
                            selected = currentRoute == navItem.screen.route,
                            onClick = {
                                navController.navigate(navItem.screen.route) {
                                    popUpTo(
                                        navController.graph.startDestinationRoute
                                            ?: Screen.CheckResult.route
                                    ) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.CheckResult.route,
                modifier = Modifier
                    .padding(innerPadding)
                    .background(Theme.colors.backGround)
            ) {
                composable(
                    route = Screen.CheckResult.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) { CheckResultScreen() }
                composable(
                    route = Screen.Timer.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }) {
                    StopWatchScreen(
                        onCheckResult = {  navController.navigate(Screen.CheckResult.route) }
                    )
                }
                composable(
                    route = Screen.Settings.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) { SettingsScreen() }
                composable(
                    route = Screen.History.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    SwimHistoryScreen(onAddSwim = { navController.navigate(Screen.AddSwimResult.route) })
                }
                composable(
                    route = Screen.AddSwimResult.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    AddSwimResultScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
