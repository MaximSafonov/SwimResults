package com.msafonov.swimresults.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.data.model.SwimCompetition
import com.msafonov.swimresults.data.model.Constants
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.history.api.NewSwimRecord
import com.msafonov.swimresults.history.api.SwimHistory
import com.msafonov.swimresults.history.api.parseTimeCentiseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class AddSwimResultUiState(
    val competitions: List<SwimCompetition> = emptyList(),
    val selectedCompetitionId: Long? = null,
    val originalCompetition: SwimCompetition? = null,
    val recordId: Long? = null,
    val loading: Boolean = false,
    val ready: Boolean = true,
    val newCompetitionName: String = "",
    val swimDate: String = todayIsoDate(),
    val swimmer: Swimmer = Swimmer.Women,
    val pool: PoolType = PoolType.Short,
    val style: SwimStyle = SwimStyle.FREESTYLE,
    val distance: Int = Constants.M50,
    val time: String = "",
    val error: AddSwimResultError? = null,
    val saving: Boolean = false,
    val saved: Boolean = false
) {
    val distances: List<Int>
        get() = when (style) {
            SwimStyle.FREESTYLE -> Constants.FREESTYLE_LIST
            SwimStyle.MEDLEY -> if (pool == PoolType.Short) Constants.MEDLEY_LIST_SHORT else Constants.MEDLEY_LIST_LONG
            else -> Constants.COMMON_LIST
        }

    val selectedCompetition: SwimCompetition?
        get() = competitions.firstOrNull { it.id == selectedCompetitionId }
            ?: originalCompetition?.takeIf { it.id == selectedCompetitionId }

    val competitionName: String
        get() = selectedCompetition?.name ?: newCompetitionName

    val canSave: Boolean
        get() = ready && !loading && !saving && !saved && competitionName.isNotBlank() && isIsoDate(swimDate) &&
            distance in distances && time.isNotBlank() &&
            parseTimeCentiseconds(time) != null
}

enum class AddSwimResultError { Validation, LoadCompetitions, LoadRecord, NotFound, SaveFailure }

private fun todayIsoDate(): String =
    Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

internal fun isIsoDate(value: String): Boolean = runCatching {
    kotlinx.datetime.LocalDate.parse(value)
}.isSuccess

class AddSwimResultViewModel(private val repository: SwimHistory) : ViewModel() {
    private val _uiState = MutableStateFlow(AddSwimResultUiState(loading = true, ready = false))
    private var initialized = false
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.competitions.catch {
                if (it is CancellationException) throw it
                _uiState.value = _uiState.value.copy(error = AddSwimResultError.LoadCompetitions)
            }.collect { _uiState.value = _uiState.value.copy(competitions = it) }
        }
    }

    fun initialize(recordId: Long?) {
        if (initialized) return
        initialized = true
        if (recordId == null) {
            _uiState.value = _uiState.value.copy(loading = false, ready = true)
            return
        }
        _uiState.value = _uiState.value.copy(recordId = recordId)
        viewModelScope.launch {
            try {
                val loaded = repository.load(recordId)
                if (loaded == null) {
                    _uiState.value = _uiState.value.copy(loading = false, error = AddSwimResultError.NotFound)
                } else {
                    val (swim, competition) = loaded
                    _uiState.value = _uiState.value.copy(
                        selectedCompetitionId = competition.id,
                        originalCompetition = competition,
                        swimDate = swim.swimDate,
                        swimmer = swim.gender,
                        pool = if (swim.poolLengthMeters == 25) PoolType.Short else PoolType.Long,
                        style = swim.style,
                        distance = swim.distanceMeters,
                        time = formatResultTime(swim.timeCentiseconds),
                        loading = false,
                        ready = true
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = AddSwimResultError.LoadRecord)
            }
        }
    }

    fun selectCompetition(id: Long?) = update {
        val selected = competitions.firstOrNull { it.id == id } ?: return@update this
        copy(selectedCompetitionId = selected.id, newCompetitionName = "",
            swimDate = if (recordId == null) selected.date else swimDate, error = null)
    }

    fun startNewCompetition() = update {
        copy(selectedCompetitionId = null, newCompetitionName = "", error = null)
    }

    fun setCompetitionName(value: String) = update { copy(newCompetitionName = value, selectedCompetitionId = null, error = null) }
    fun setSwimDate(value: String) = update { copy(swimDate = value, error = null) }
    fun setSwimmer(value: Swimmer) = update { copy(swimmer = value) }
    fun setPool(value: PoolType) = update { copy(pool = value, distance = normalizedDistance(distance, style, value)) }
    fun setStyle(value: SwimStyle) = update { copy(style = value, distance = normalizedDistance(distance, value, pool)) }
    fun setDistance(value: Int) = update { if (value in distances) copy(distance = value) else this }
    fun setTime(value: String) = update { copy(time = value.filter { it.isDigit() || it == ':' || it == '.' }.take(8), error = null) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.saving || state.saved || !state.ready) return
        if (!state.canSave) {
            _uiState.value = state.copy(error = AddSwimResultError.Validation)
            return
        }
        _uiState.value = state.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                val input = NewSwimRecord(
                    competitionName = state.competitionName,
                    swimDate = state.swimDate,
                    distanceMeters = state.distance,
                    style = state.style,
                    gender = state.swimmer,
                    pool = state.pool,
                    time = state.time
                )
                if (state.recordId == null) repository.save(input)
                else repository.update(state.recordId, input, state.selectedCompetitionId)
                _uiState.value = _uiState.value.copy(saving = false, saved = true)
            } catch (cancelled: CancellationException) {
                _uiState.value = _uiState.value.copy(saving = false)
                throw cancelled
            } catch (_: NoSuchElementException) {
                _uiState.value = _uiState.value.copy(saving = false, ready = false, error = AddSwimResultError.NotFound)
                return@launch
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = AddSwimResultError.SaveFailure)
                return@launch
            }
            onSaved()
        }
    }

    private fun update(block: AddSwimResultUiState.() -> AddSwimResultUiState) {
        if (_uiState.value.ready && !_uiState.value.saving && !_uiState.value.saved) {
            _uiState.value = _uiState.value.block()
        }
    }

    private fun formatResultTime(centiseconds: Long): String {
        val minutes = centiseconds / 6000
        val seconds = (centiseconds / 100 % 60).toString().padStart(2, '0')
        val fraction = (centiseconds % 100).toString().padStart(2, '0')
        return "$minutes:$seconds.$fraction"
    }

    private fun normalizedDistance(distance: Int, style: SwimStyle, pool: PoolType): Int {
        val allowed = when (style) {
            SwimStyle.FREESTYLE -> Constants.FREESTYLE_LIST
            SwimStyle.MEDLEY -> if (pool == PoolType.Short) Constants.MEDLEY_LIST_SHORT else Constants.MEDLEY_LIST_LONG
            else -> Constants.COMMON_LIST
        }
        return distance.takeIf { it in allowed } ?: allowed.first()
    }
}
