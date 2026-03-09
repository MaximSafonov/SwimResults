package com.msafonov.swimresults

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.msafonov.swimresults.di.KoinApp
import com.msafonov.swimresults.ui.screens.SwimResultsApp
import com.msafonov.swimresults.ui.theme.SwimRankAppTheme
import org.koin.compose.KoinApplication
import org.koin.plugin.module.dsl.koinConfiguration

@Composable
@Preview
fun App() {
    KoinApplication(configuration = koinConfiguration<KoinApp>()) {
        SwimRankAppTheme {
            SwimResultsApp()
        }
    }
}