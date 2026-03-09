package com.msafonov.swimresults.data.repository

import com.msafonov.swimresults.data.storage.SettingsStorage
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Singleton

@Singleton
class SettingsRepository(
    private val settingsStorage: SettingsStorage
) {
    val isDark: Flow<Boolean> = settingsStorage.isDark

    fun toggleTheme() = settingsStorage.toggleTheme()

}