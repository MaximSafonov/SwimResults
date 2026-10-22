package com.msafonov.swimresults.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import swimresults.swimresultsapp.generated.resources.Res
import swimresults.swimresultsapp.generated.resources.resultScreenTitle
import swimresults.swimresultsapp.generated.resources.settingsScreenTitle
import swimresults.swimresultsapp.generated.resources.stopWatchScreenTitle
import swimresults.swimresultsapp.generated.resources.historyScreenTitle

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val title: StringResource
)

val bottomNavItems = listOf<BottomNavItem>(
    BottomNavItem(
        screen = Screen.CheckResult,
        icon = Icons.Default.Home,
        title = Res.string.resultScreenTitle
    ),
    BottomNavItem(
        screen = Screen.Timer,
        icon = Icons.Default.PlayArrow,
        title = Res.string.stopWatchScreenTitle
    ),
    BottomNavItem(
        screen = Screen.History,
        icon = Icons.Default.History,
        title = Res.string.historyScreenTitle
    ),
    BottomNavItem(
        screen = Screen.Settings,
        icon = Icons.Default.Settings,
        title = Res.string.settingsScreenTitle
    ),
)
