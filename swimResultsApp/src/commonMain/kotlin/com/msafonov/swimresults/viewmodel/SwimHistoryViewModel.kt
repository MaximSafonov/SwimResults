package com.msafonov.swimresults.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.data.model.SwimCompetitionGroup
import com.msafonov.swimresults.data.repository.SwimHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class SwimHistoryUiState(
    val groups: List<SwimCompetitionGroup> = emptyList(),
    val loading: Boolean = true,
    val error: Boolean = false
)

@KoinViewModel
class SwimHistoryViewModel(private val repository: SwimHistoryRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SwimHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.history
                .catch { _uiState.value = _uiState.value.copy(loading = false, error = true) }
                .collect { _uiState.value = SwimHistoryUiState(groups = it, loading = false) }
        }
    }
}
