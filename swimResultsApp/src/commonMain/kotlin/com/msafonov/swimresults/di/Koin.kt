package com.msafonov.swimresults.di

import com.msafonov.swimresults.data.repository.CheckResultRepository
import com.msafonov.swimresults.data.repository.SettingsRepository
import com.msafonov.swimresults.data.repository.StopWatchRepository
import com.msafonov.swimresults.data.storage.InMemorySettingsStorage
import com.msafonov.swimresults.data.storage.InMemoryStopWatchStorage
import com.msafonov.swimresults.data.storage.RankTableStorage
import com.msafonov.swimresults.data.storage.SettingsStorage
import com.msafonov.swimresults.data.storage.StopWatchStorage
import com.msafonov.swimresults.viewmodel.CheckResultViewModel
import com.msafonov.swimresults.viewmodel.SettingsViewModel
import com.msafonov.swimresults.viewmodel.StopWatchViewModel
import org.koin.core.annotation.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes
import org.koin.dsl.module

@KoinApplication
object KoinApp

val dataModule = module {
    single<RankTableStorage> { RankTableStorage() }
    single { CheckResultRepository(get(), get()) }

    single<SettingsStorage> { InMemorySettingsStorage() }
    single { SettingsRepository(get()) }

    single<StopWatchStorage> { InMemoryStopWatchStorage() }
    single { StopWatchRepository(get()) }
}

val viewModelModule = module {
    factoryOf(::CheckResultViewModel)
    factoryOf(::SettingsViewModel)
    factoryOf(::StopWatchViewModel)
}

val appModule = module {
    includes(dataModule, viewModelModule)
}

fun initKoin(configuration: KoinAppDeclaration? = null) {
    startKoin {
        includes(configuration)
        modules(appModule)
    }
}