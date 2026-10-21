package com.msafonov.swimresults.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.RankTableType
import com.msafonov.swimresults.data.model.SwimEvent
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.repository.CheckResultRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class CheckResultViewModel(
    private val repository: CheckResultRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckResultUiState())
    val uiState = _uiState.asStateFlow()
    private val tables = mutableMapOf<RankTableType, List<SwimEvent>>()
    private var tableJob: Job? = null
    private var stopwatchJob: Job? = null
    private var generation = 0L

    // Internal test seam: set before the queued initial load runs. DI stays unchanged.
    internal var tableDispatcher: CoroutineDispatcher = Dispatchers.Default

    // Compatibility adapters for the existing auxiliary components; all state is derived.
    private fun <T> selection(select: (CheckResultUiState) -> T) = uiState.map(select)
        .distinctUntilChanged().stateIn(
            viewModelScope, SharingStarted.Eagerly, select(uiState.value)
        )
    val result = selection { it.result }
    val timerInput = selection { it.timerInput }
    val poolType = selection { it.filters.poolType }
    val swimmer = selection { it.filters.swimmer }
    val distance = selection { it.filters.distance }
    val swimStyle = selection { it.filters.swimStyle }
    val eventTable = selection { it.eventTable }
    val currentEvent = selection { it.currentEvent }
    val distanceList = selection { it.filters.distances }
    val stopWatchTime = repository.stopWatchTime.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(500), null
    )

    init { loadTable() }

    // Only the resumed route may consume the shared handoff. Older back-stack VMs stay idle.
    fun startStopwatchUpdates() {
        if (stopwatchJob?.isActive == true) return
        stopwatchJob = viewModelScope.launch {
            repository.stopWatchTime.collect { time ->
                if (time != null) {
                    _uiState.value = _uiState.value.copy(
                        timerInput = TextFieldValue(time, TextRange(time.length)),
                        resultExpanded = true,
                        resultDetails = null
                    )
                    repository.dropTime()
                }
            }
        }
    }

    fun stopStopwatchUpdates() {
        stopwatchJob?.cancel()
        stopwatchJob = null
    }

    fun checkPool(poolType: PoolType) = selectFilters(_uiState.value.filters.copy(poolType = poolType))
    fun setSwimmer(swimmer: Swimmer) = selectFilters(_uiState.value.filters.copy(swimmer = swimmer))
    fun setSwimStyle(swimStyle: SwimStyle) =
        selectFilters(_uiState.value.filters.copy(swimStyle = swimStyle))

    fun setDistance(distance: Int) {
        if (distance in _uiState.value.filters.distances) {
            selectFilters(_uiState.value.filters.copy(distance = distance))
        }
    }

    private fun selectFilters(candidate: CheckResultFilters) {
        val before = _uiState.value
        val filters = candidate.normalized()
        if (filters == before.filters) return
        val next = before.copy(filters = filters, resultDetails = null)
        if (filters.tableType == before.filters.tableType) {
            _uiState.value = if (before.status == StandardsStatus.Ready ||
                before.status == StandardsStatus.Unavailable
            ) next.withTable(before.eventTable) else next
        } else {
            loadTable(next)
        }
    }

    private fun loadTable(state: CheckResultUiState = _uiState.value) {
        tableJob?.cancel()
        val request = ++generation
        val key = state.filters.tableType
        tables[key]?.let {
            _uiState.value = state.withTable(it)
            return
        }
        _uiState.value = state.copy(
            status = StandardsStatus.Loading, eventTable = emptyList(), currentEvent = null,
            standards = qualificationStandards(null), resultDetails = null
        )
        tableJob = viewModelScope.launch {
            try {
                val table = withContext(tableDispatcher) { repository.rankTable(key) }
                if (request == generation && key == _uiState.value.filters.tableType) {
                    tables[key] = table
                    // Style/distance may have changed while this table was being loaded.
                    _uiState.value = _uiState.value.withTable(table)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (request == generation && key == _uiState.value.filters.tableType) {
                    _uiState.value = _uiState.value.copy(status = StandardsStatus.Error)
                }
            }
        }
    }

    fun retry() {
        if (_uiState.value.status == StandardsStatus.Error) loadTable()
    }

    fun onInputChanged(newValue: TextFieldValue) {
        if (_uiState.value.timerInput == newValue) return
        _uiState.value = _uiState.value.copy(timerInput = newValue, resultDetails = null)
    }

    fun toggleResultExpanded() {
        _uiState.value = _uiState.value.copy(resultExpanded = !_uiState.value.resultExpanded)
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(resultDetails = null)
    }

    fun checkResult() {
        val state = _uiState.value
        if (!state.canCheckResult) return
        val checked = repository.checkResult(
            eventTable = state.eventTable, swimStyle = state.filters.swimStyle,
            distance = state.filters.distance, result = state.timerInput.text
        )
        _uiState.value = state.copy(
            resultDetails = buildResultDetails(state.filters, state.timerInput.text, state.currentEvent, checked)
        )
    }
}
