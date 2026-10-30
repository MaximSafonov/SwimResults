package com.msafonov.swimresults.stopwatch.di

import com.msafonov.swimresults.data.storage.InMemoryStopWatchStorage
import com.msafonov.swimresults.data.storage.StopWatchStorage
import com.msafonov.swimresults.viewmodel.StopWatchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val stopwatchModule = module {
    single<StopWatchStorage> { InMemoryStopWatchStorage() }
    viewModel { StopWatchViewModel(get()) }
}
