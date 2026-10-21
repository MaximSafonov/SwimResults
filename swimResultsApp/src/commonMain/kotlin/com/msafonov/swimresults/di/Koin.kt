package com.msafonov.swimresults.di

import com.msafonov.swimresults.data.local.SettingsDataStoreOwner
import com.msafonov.swimresults.data.local.getSettingsDataStoreOwner
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@KoinApplication
object KoinApp

@Module
@ComponentScan("com.msafonov.swimresults")
@Configuration
class AppModule {
    @Single
    internal fun provideSettingsDataStoreOwner(): SettingsDataStoreOwner =
        getSettingsDataStoreOwner()
}
