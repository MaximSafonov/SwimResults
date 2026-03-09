package com.msafonov.swimresults.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(
    private val repository: SettingsRepository
): ViewModel() {
    var isDark: StateFlow<Boolean> = repository.isDark.stateIn(viewModelScope, SharingStarted.WhileSubscribed(500), false)
    fun toggleTheme() = repository.toggleTheme()
}