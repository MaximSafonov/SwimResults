package com.msafonov.swimresults.ui.navigation


import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import swimresults.swimresultsapp.generated.resources.Res
import swimresults.swimresultsapp.generated.resources.resultScreenTitle
import swimresults.swimresultsapp.generated.resources.settingsScreenTitle
import swimresults.swimresultsapp.generated.resources.stopWatchScreenTitle
import swimresults.swimresultsapp.generated.resources.historyScreenTitle
import swimresults.swimresultsapp.generated.resources.addSwimTitle
import swimresults.swimresultsapp.generated.resources.editSwimTitle


@Serializable
sealed class Screen() {
    abstract val route: String
    abstract suspend fun title(): String
    @Serializable
    data object CheckResult : Screen() {
        override val route: String = "checkResult"
        override suspend fun  title(): String = getString(Res.string.resultScreenTitle)
    }
    @Serializable
    data object Timer : Screen() {
        override val route: String = "timer"
        override suspend fun title(): String = getString(Res.string.stopWatchScreenTitle)
    }
    @Serializable
    data object History : Screen() {
        override val route: String = "history"
        override suspend fun title(): String = getString(Res.string.historyScreenTitle)
    }
    @Serializable
    data object AddSwimResult : Screen() {
        override val route: String = "addSwimResult"
        override suspend fun title(): String = getString(Res.string.addSwimTitle)
    }
    @Serializable
    data object EditSwimResult : Screen() {
        override val route: String = "editSwimResult/{recordId}"
        fun route(recordId: Long): String = "editSwimResult/$recordId"
        override suspend fun title(): String = getString(Res.string.editSwimTitle)
    }
    @Serializable
    data object Settings : Screen() {
        override val route: String = "settings"
        override suspend fun  title(): String = getString(Res.string.settingsScreenTitle)

    }
}



