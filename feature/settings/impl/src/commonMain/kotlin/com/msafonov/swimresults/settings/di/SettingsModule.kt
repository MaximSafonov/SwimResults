package com.msafonov.swimresults.settings.di

import com.msafonov.swimresults.data.local.getSettingsDataStoreOwner
import com.msafonov.swimresults.data.repository.SettingsRepository
import com.msafonov.swimresults.data.storage.DataStoreSettingsStorage
import com.msafonov.swimresults.data.storage.SettingsStorage
import com.msafonov.swimresults.settings.api.Settings
import com.msafonov.swimresults.viewmodel.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    single { getSettingsDataStoreOwner() }
    single<SettingsStorage> { DataStoreSettingsStorage(get()) }
    single<Settings> { SettingsRepository(get()) }
    viewModel { SettingsViewModel(get()) }
}
