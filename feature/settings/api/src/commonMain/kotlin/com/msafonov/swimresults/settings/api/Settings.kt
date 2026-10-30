package com.msafonov.swimresults.settings.api

import kotlinx.coroutines.flow.Flow

interface Settings {
    val isDark: Flow<Boolean>
    fun toggleTheme()
}
