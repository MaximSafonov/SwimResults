package com.msafonov.swimresults.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.settings.api.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val repository: Settings
): ViewModel() {
    var isDark: StateFlow<Boolean> = repository.isDark.stateIn(viewModelScope, SharingStarted.WhileSubscribed(500), false)
    fun toggleTheme() = repository.toggleTheme()
}