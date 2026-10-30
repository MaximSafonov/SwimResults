package com.msafonov.swimresults.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.data.model.SwimCompetitionGroup
import com.msafonov.swimresults.history.api.SwimHistory
import com.msafonov.swimresults.history.api.parseTimeCentiseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class SwimHistoryUiState(
    val groups: List<SwimCompetitionGroup> = emptyList(),
    val loading: Boolean = true,
    val error: Boolean = false,
    val deletingIds: Set<Long> = emptySet(),
    val actionError: Boolean = false
)

class SwimHistoryViewModel(private val repository: SwimHistory) : ViewModel() {
    private val _uiState = MutableStateFlow(SwimHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.history
                .catch {
                    if (it is CancellationException) throw it
                    _uiState.value = _uiState.value.copy(loading = false, error = true)
                }
                .collect { groups ->
                    val visibleIds = groups.flatMap { group -> group.swims.map { it.id } }.toSet()
                    _uiState.value = _uiState.value.copy(
                        groups = groups, loading = false, error = false,
                        deletingIds = _uiState.value.deletingIds.intersect(visibleIds)
                    )
                }
        }
    }

    fun delete(id: Long) {
        val state = _uiState.value
        if (id in state.deletingIds || state.groups.none { group -> group.swims.any { it.id == id } }) return
        _uiState.value = state.copy(deletingIds = state.deletingIds + id, actionError = false)
        viewModelScope.launch {
            try {
                // Keep the row disabled until the storage Flow removes it.
                repository.delete(id)
            } catch (cancelled: CancellationException) {
                _uiState.value = _uiState.value.copy(deletingIds = _uiState.value.deletingIds - id)
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    actionError = true, deletingIds = _uiState.value.deletingIds - id
                )
            }
        }
    }
}
