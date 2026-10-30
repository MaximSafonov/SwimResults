package com.msafonov.swimresults.checkresult.di

import com.msafonov.swimresults.checkresult.api.CheckResult
import com.msafonov.swimresults.data.repository.CheckResultRepository
import com.msafonov.swimresults.data.storage.RankTable
import com.msafonov.swimresults.data.storage.RankTableStorage
import com.msafonov.swimresults.viewmodel.CheckResultViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val checkResultModule = module {
    single<RankTable> { RankTableStorage() }
    single<CheckResult> { CheckResultRepository(get(), get()) }
    viewModel { CheckResultViewModel(get()) }
}
