package com.msafonov.swimresults.history.di

import com.msafonov.swimresults.data.local.buildSwimHistoryDatabase
import com.msafonov.swimresults.data.repository.SwimHistoryRepository
import com.msafonov.swimresults.history.api.SwimHistory
import com.msafonov.swimresults.viewmodel.AddSwimResultViewModel
import com.msafonov.swimresults.viewmodel.SwimHistoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val historyModule = module {
    viewModel { SwimHistoryViewModel(get()) }
    viewModel { AddSwimResultViewModel(get()) }
    single { buildSwimHistoryDatabase() }
    single { get<com.msafonov.swimresults.data.local.SwimHistoryDatabase>().swimHistoryDao() }
    single<SwimHistory> { SwimHistoryRepository(get(), get()) }
}
