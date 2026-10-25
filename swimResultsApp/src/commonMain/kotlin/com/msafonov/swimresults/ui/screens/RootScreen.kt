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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.msafonov.swimresults.ui.navigation.Screen
import com.msafonov.swimresults.ui.navigation.bottomNavItems
import com.msafonov.swimresults.ui.theme.SwimRankAppTheme
import com.msafonov.swimresults.ui.theme.Theme
import com.msafonov.swimresults.viewmodel.SettingsViewModel
import com.msafonov.swimresults.viewmodel.AddSwimResultViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SwimResultsApp() {
    val navController = rememberNavController()
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val isDark by settingsViewModel.isDark.collectAsStateWithLifecycle()
    val profileViewModel = koinViewModel<com.msafonov.swimresults.viewmodel.ProfileViewModel>()
    SwimRankAppTheme(isDark) {
        Scaffold(
            containerColor = Theme.colors.backGround,
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                if (currentRoute != Screen.AddSwimResult.route && currentRoute != Screen.EditSwimResult.route &&
                    currentRoute != Screen.AddAthlete.route && currentRoute != Screen.EditAthlete.route) {
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
                            selected = currentRoute == navItem.screen.route || (navItem.screen == Screen.Profile && currentRoute == Screen.Settings.route),
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
                ) { SettingsScreen(onBack = { navController.popBackStack() }) }
                composable(Screen.Profile.route) {
                    ProfileScreen(onSettings = { navController.navigate(Screen.Settings.route) },
                        onAddAthlete = { navController.navigate(Screen.AddAthlete.route) },
                        onEditAthlete = { navController.navigate(Screen.EditAthlete.route(it)) }, viewModel = profileViewModel)
                }
                composable(Screen.AddAthlete.route) { entry ->
                    AthleteFormScreen(onBack = { if (navController.currentBackStackEntry == entry) navController.popBackStack() },
                        viewModel = koinViewModel(viewModelStoreOwner = entry))
                }
                composable(Screen.EditAthlete.route, arguments = listOf(navArgument("athleteId") { type = NavType.LongType })) { entry ->
                    AthleteFormScreen(onBack = { if (navController.currentBackStackEntry == entry) navController.popBackStack() },
                        athleteId = checkNotNull(NavType.LongType[checkNotNull(entry.arguments), "athleteId"]),
                        viewModel = koinViewModel(viewModelStoreOwner = entry))
                }
                composable(
                    route = Screen.History.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    SwimHistoryScreen(
                        onProfile = { navController.navigate(Screen.Profile.route) },
                        onAddSwim = { navController.navigate(Screen.AddSwimResult.route) { launchSingleTop = true } },
                        onEditSwim = { id -> navController.navigate(Screen.EditSwimResult.route(id)) { launchSingleTop = true } }
                    )
                }
                composable(
                    route = Screen.AddSwimResult.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) { entry ->
                    AddSwimResultScreen(
                        onBack = {
                            if (navController.currentBackStackEntry == entry) {
                                navController.popBackStack()
                            }
                        },
                        viewModel = koinViewModel<AddSwimResultViewModel>(viewModelStoreOwner = entry)
                    )
                }
                composable(
                    route = Screen.EditSwimResult.route,
                    arguments = listOf(navArgument("recordId") { type = NavType.LongType }),
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) { entry ->
                    AddSwimResultScreen(
                        recordId = checkNotNull(NavType.LongType[checkNotNull(entry.arguments), "recordId"]),
                        onBack = {
                            if (navController.currentBackStackEntry == entry) {
                                navController.popBackStack()
                            }
                        },
                        viewModel = koinViewModel<AddSwimResultViewModel>(viewModelStoreOwner = entry)
                    )
                }
            }
        }
    }
}
