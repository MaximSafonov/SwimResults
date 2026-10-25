package com.msafonov.swimresults.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.auth.SessionSnapshot
import com.msafonov.swimresults.data.model.AthleteBirthDate
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

enum class AthleteFormError { SessionChanged, Unavailable, LoadFailed, InvalidInput, SaveFailed }

data class AthleteFormUiState(
    val name: String = "", val birthDate: String = "", val gender: Swimmer? = null, val rank: SwimmingRank? = null,
    val loading: Boolean = true, val saving: Boolean = false, val saved: Boolean = false,
    val validSession: Boolean = true, val error: AthleteFormError? = null, val dirty: Boolean = false
) {
    val validBirthDate: Boolean get() = AthleteBirthDate.isValid(birthDate)
    val valid: Boolean get() = name.trim().length in 1..80 && validBirthDate
}

private data class AthleteDraft(val name: String = "", val birthDate: String = "", val gender: Swimmer? = null, val rank: SwimmingRank? = null)
private fun AthleteFormUiState.draft() = AthleteDraft(name, birthDate, gender, rank)

@KoinViewModel
class AthleteFormViewModel(private val repository: ProfileRepository, private val savedState: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(AthleteFormUiState())
    val uiState = mutable.asStateFlow()
    val session = repository.session.state
    fun isCurrent(current: SessionSnapshot): Boolean = snapshot == null || snapshot == current
    private var snapshot: SessionSnapshot? = null
    private var athleteId: Long? = null
    private var initialized = false
    private var original = AthleteDraft()

    init {
        viewModelScope.launch { repository.session.state.collect { current ->
            if (snapshot != null && snapshot != current) {
                mutable.value = AthleteFormUiState(loading = false, validSession = false, error = AthleteFormError.SessionChanged)
                original = AthleteDraft()
                savedState.remove<Long>("draftId")
                savedState.remove<String>("draftAccount")
                savedState.remove<String>("draftName")
                savedState.remove<String>("draftBirthDate")
                savedState.remove<String>("draftRank")
                savedState.remove<String>("draftGender")
            }
        } }
    }

    fun initialize(id: Long?) {
        if (initialized) return
        initialized = true
        athleteId = id
        val captured = repository.session.current
        snapshot = captured
        if (captured.accountId == null) {
            mutable.value = AthleteFormUiState(loading = false, validSession = false)
            return
        }
        viewModelScope.launch {
            try {
                val existing = id?.let { repository.load(captured, it) }
                if (!repository.session.isCurrent(captured)) return@launch
                if (id != null && existing == null) {
                    mutable.value = AthleteFormUiState(loading = false, validSession = false, error = AthleteFormError.Unavailable)
                    return@launch
                }
                original = AthleteDraft(existing?.name.orEmpty(), existing?.birthDate?.let(AthleteBirthDate::format).orEmpty(), existing?.sportsGender, existing?.rank)
                val restored = savedState.get<String>("draftAccount") == captured.accountId && savedState.get<Long>("draftId") == (id ?: 0L)
                val name = if (restored) savedState.get<String>("draftName").orEmpty() else original.name
                val birthDate = if (restored) savedState.get<String>("draftBirthDate").orEmpty() else original.birthDate
                val gender = if (restored) savedState.get<String>("draftGender")?.let { value -> Swimmer.entries.firstOrNull { it.name == value } } else original.gender
                val rank = if (restored) savedState.get<String>("draftRank")?.let { value -> SwimmingRank.entries.firstOrNull { it.name == value } } else original.rank
                mutable.value = AthleteFormUiState(name, birthDate, gender, rank, loading = false, dirty = AthleteDraft(name, birthDate, gender, rank) != original)
                persist()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (repository.session.isCurrent(captured)) mutable.value = mutable.value.copy(loading = false, error = AthleteFormError.LoadFailed) }
        }
    }

    fun name(value: String) = edit { copy(name = value.take(80)) }
    fun birthDate(value: String) = edit { copy(birthDate = value.take(32)) }
    fun rank(value: SwimmingRank?) = edit { copy(rank = value) }
    fun gender(value: Swimmer?) = edit { copy(gender = value) }
    private fun edit(block: AthleteFormUiState.() -> AthleteFormUiState) {
        if (mutable.value.loading || mutable.value.saving || mutable.value.saved || !mutable.value.validSession || snapshot?.let { !repository.session.isCurrent(it) } == true) return
        val next = mutable.value.block()
        mutable.value = next.copy(error = null, dirty = next.draft() != original)
        persist()
    }

    private fun persist() {
        savedState["draftAccount"] = snapshot?.accountId
        savedState["draftId"] = athleteId ?: 0L
        savedState["draftName"] = mutable.value.name
        savedState["draftBirthDate"] = mutable.value.birthDate
        savedState["draftRank"] = mutable.value.rank?.name
        savedState["draftGender"] = mutable.value.gender?.name
    }

    fun save(onSaved: () -> Unit) {
        val state = mutable.value
        val captured = snapshot ?: return
        if (state.loading || state.saving || state.saved || !state.validSession || !repository.session.isCurrent(captured)) return
        if (!state.valid) { mutable.value = state.copy(error = AthleteFormError.InvalidInput); return }
        mutable.value = state.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                repository.save(captured, athleteId, state.name, state.birthDate, state.gender, state.rank)
                if (!repository.session.isCurrent(captured)) return@launch
                mutable.value = mutable.value.copy(saving = false, saved = true, dirty = false)
                onSaved()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (repository.session.isCurrent(captured)) mutable.value = mutable.value.copy(error = AthleteFormError.SaveFailed) }
            finally { mutable.value = mutable.value.copy(saving = false) }
        }
    }
}
