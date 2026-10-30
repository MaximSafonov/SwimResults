package com.msafonov.swimresults.data.repository

import com.msafonov.swimresults.data.storage.SettingsStorage
import com.msafonov.swimresults.settings.api.Settings
import kotlinx.coroutines.flow.Flow

internal class SettingsRepository(
    private val settingsStorage: SettingsStorage
) : Settings {
    override val isDark: Flow<Boolean> = settingsStorage.isDark

    override fun toggleTheme() = settingsStorage.toggleTheme()
}
