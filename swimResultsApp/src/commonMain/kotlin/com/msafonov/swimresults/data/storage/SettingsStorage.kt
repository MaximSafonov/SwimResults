package com.msafonov.swimresults.data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface SettingsStorage {
    val isDark: Flow<Boolean>
    fun toggleTheme()
}

class InMemorySettingsStorage: SettingsStorage {
    override val isDark = MutableStateFlow(false)
    override fun toggleTheme() { isDark.value = !isDark.value }
}