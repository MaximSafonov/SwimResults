package com.msafonov.swimresults

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.msafonov.swimresults.data.local.InitializeSettingsStorage
import com.msafonov.swimresults.data.local.InitializeSwimHistoryStorage
import com.msafonov.swimresults.di.KoinApp
import com.msafonov.swimresults.core.di.composeModules
import com.msafonov.swimresults.settings.di.settingsModule
import com.msafonov.swimresults.stopwatch.di.stopwatchModule
import com.msafonov.swimresults.checkresult.di.checkResultModule
import com.msafonov.swimresults.history.di.historyModule
import com.msafonov.swimresults.ui.screens.SwimResultsApp
import com.msafonov.swimresults.ui.theme.SwimRankAppTheme
import org.koin.compose.KoinApplication
import org.koin.plugin.module.dsl.koinConfiguration

@Composable
@Preview
fun App() {
    InitializeSettingsStorage()
    InitializeSwimHistoryStorage()
    KoinApplication(configuration = composeModules(koinConfiguration<KoinApp>(), settingsModule, stopwatchModule, checkResultModule, historyModule)) {
        SwimRankAppTheme {
            SwimResultsApp()
        }
    }
}
