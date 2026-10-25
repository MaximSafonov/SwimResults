package com.msafonov.swimresults.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.auth.SessionSnapshot
import com.msafonov.swimresults.data.model.Athlete
import com.msafonov.swimresults.data.model.HistoryLabel
import com.msafonov.swimresults.data.model.HistoryContext
import com.msafonov.swimresults.data.model.RecordedSwim
import com.msafonov.swimresults.data.model.SwimCompetitionGroup
import com.msafonov.swimresults.data.repository.ProfileRepository
import com.msafonov.swimresults.data.repository.SwimHistoryRepository
import com.msafonov.swimresults.data.model.SwimCompetition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class LinkConfirmation(val session: SessionSnapshot, val target: Athlete, val rows: List<RecordedSwim>) {
    val recordIds: List<Long> get() = rows.map { it.id }
}

sealed interface HistoryMessage {
    data object AthleteChanged : HistoryMessage
    data object RecordsChanged : HistoryMessage
    data class SelectionLimit(val limit: Int) : HistoryMessage
    data class Linked(val count: Int, val athleteName: String) : HistoryMessage
    data object LinkFailed : HistoryMessage
}

data class SwimHistoryUiState(
    val groups: List<SwimCompetitionGroup> = emptyList(), val loading: Boolean = true,
    val error: Boolean = false, val deletingIds: Set<Long> = emptySet(), val actionError: Boolean = false,
    val context: HistoryContext? = null, val athletes: List<Athlete> = emptyList(),
    val choosingAthlete: Boolean = false,
    val selecting: Boolean = false, val selectedIds: Set<Long> = emptySet(), val targetId: Long? = null,
    val confirmation: LinkConfirmation? = null, val linking: Boolean = false, val message: HistoryMessage? = null
) {
    val canLink: Boolean get() = context?.session?.accountId != null && context.athleteId == null && context.available
}

@KoinViewModel
class SwimHistoryViewModel(private val repository: SwimHistoryRepository, private val profiles: ProfileRepository) : ViewModel() {
    private val mutable = MutableStateFlow(SwimHistoryUiState())
    val uiState = mutable.asStateFlow()
    val session = repository.session.state

    private val contextRequests = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            // A failed cold database Flow completes. Only a new session or explicit retry
            // starts a fresh attempt; choosing an unchanged context does nothing.
            combine(session, contextRequests) { snapshot, _ -> snapshot }.collectLatest { snapshot ->
                mutable.value = SwimHistoryUiState()
                try {
                    // Both profile subscriptions belong to this attempt. Failure of either
                    // cancels the other before clearing data, so late emissions cannot refill it.
                    coroutineScope {
                        launch {
                            profiles.profiles.collect { profile ->
                                val context = mutable.value.context ?: return@collect
                                if (profile.session != snapshot || context.session != snapshot || !isCurrent(context)) return@collect
                                val confirmation = mutable.value.confirmation
                                val changed = confirmation != null && profile.athletes.none { it == confirmation.target }
                                mutable.value = mutable.value.copy(athletes = profile.athletes,
                                    targetId = mutable.value.targetId?.takeIf { id -> profile.athletes.any { it.id == id } },
                                    confirmation = confirmation.takeUnless { changed },
                                    message = if (changed) HistoryMessage.AthleteChanged else mutable.value.message)
                            }
                        }
                        repository.contexts.collectLatest contextCollector@ { context ->
                            if (context.session != snapshot || !repository.session.isCurrent(snapshot)) return@contextCollector
                            val previous = mutable.value
                            mutable.value = SwimHistoryUiState(context = context,
                                athletes = previous.athletes.takeIf { previous.context?.session == snapshot }.orEmpty(),
                                choosingAthlete = previous.choosingAthlete)
                            if (snapshot.accountId != null) {
                                val profile = profiles.current(snapshot)
                                if (isCurrent(context)) mutable.value = mutable.value.copy(athletes = profile.athletes)
                            }
                            repository.history(context).collect { groups ->
                                if (!isCurrent(context)) return@collect
                                val previous = mutable.value
                                val rows = groups.flatMap { it.swims }
                                val ids = rows.map { it.id }.toSet()
                                val removedSelection = !ids.containsAll(previous.selectedIds)
                                val changed = removedSelection || (previous.confirmation?.rows?.any { expected -> rows.firstOrNull { it.id == expected.id } != expected } == true)
                                mutable.value = previous.copy(groups = groups, loading = false, error = false,
                                    deletingIds = previous.deletingIds.intersect(ids),
                                    selectedIds = previous.selectedIds.intersect(ids),
                                    confirmation = previous.confirmation.takeUnless { changed },
                                    message = if (changed) HistoryMessage.RecordsChanged else previous.message)
                            }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (repository.session.isCurrent(snapshot)) {
                        mutable.value = SwimHistoryUiState(
                            context = HistoryContext(snapshot, label = HistoryLabel.LoadError, available = false),
                            loading = false, error = true
                        )
                    }
                }
            }
        }
    }

    fun isCurrent(context: HistoryContext): Boolean = repository.session.isCurrent(context.session) && mutable.value.context == context
    fun deviceLocal(value: Boolean) {
        val state = mutable.value
        val context = state.context ?: return
        if (!isCurrent(context) || state.loading || state.error || state.linking || state.choosingAthlete) return
        if (context.available && (context.athleteId == null) == value) return
        repository.showDeviceLocal(value)
    }

    fun selectAthlete(id: Long) {
        val state = mutable.value
        val context = state.context ?: return
        if (!isCurrent(context) || state.loading || state.error || state.linking || state.choosingAthlete ||
            state.athletes.none { it.id == id && it.accountId == context.session.accountId }) return
        // Keep loaded rows, selection, menus and scroll untouched on a repeated tap.
        if (context.available && context.athleteId == id) return
        mutable.value = state.copy(choosingAthlete = true, actionError = false)
        viewModelScope.launch {
            try {
                profiles.select(context.session, id)
                if (repository.session.isCurrent(context.session)) repository.showDeviceLocal(false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (isCurrent(context)) mutable.value = mutable.value.copy(actionError = true)
            } finally {
                if (repository.session.isCurrent(context.session)) mutable.value = mutable.value.copy(choosingAthlete = false)
            }
        }
    }

    fun retry() {
        val state = mutable.value
        if (!state.error || state.loading || state.context?.session != repository.session.current) return
        contextRequests.value++
    }
    fun startSelection() {
        if (!mutable.value.canLink || mutable.value.linking || mutable.value.choosingAthlete) return
        mutable.value = mutable.value.copy(selecting = true, selectedIds = emptySet(), confirmation = null, message = null)
    }
    fun cancelSelection() {
        if (!mutable.value.linking) mutable.value = mutable.value.copy(selecting = false, selectedIds = emptySet(), targetId = null, confirmation = null)
    }
    fun toggle(id: Long) {
        val state = mutable.value
        if (!state.selecting || state.linking || state.groups.none { g -> g.swims.any { it.id == id } }) return
        val ids = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
        if (ids.size > 200) { mutable.value = state.copy(message = HistoryMessage.SelectionLimit(200)); return }
        mutable.value = state.copy(selectedIds = ids, confirmation = null, message = null)
    }
    fun target(id: Long) {
        if (!mutable.value.linking && mutable.value.athletes.any { it.id == id }) mutable.value = mutable.value.copy(targetId = id, confirmation = null)
    }
    fun prepareLink() {
        val state = mutable.value
        val context = state.context ?: return
        val target = state.athletes.firstOrNull { it.id == state.targetId } ?: return
        if (!state.canLink || !isCurrent(context) || state.selectedIds.isEmpty() || state.linking || state.choosingAthlete) return
        val rows = state.groups.flatMap { it.swims }.filter { it.id in state.selectedIds }
        if (rows.size != state.selectedIds.size) return
        mutable.value = state.copy(confirmation = LinkConfirmation(context.session, target, rows.toList()))
    }
    fun dismissConfirmation() { if (!mutable.value.linking) mutable.value = mutable.value.copy(confirmation = null) }
    fun confirmLink() {
        val state = mutable.value
        val confirmation = state.confirmation ?: return
        val context = state.context ?: return
        if (state.linking || state.choosingAthlete || !state.canLink || !isCurrent(context) || confirmation.session != context.session) return
        mutable.value = state.copy(linking = true)
        viewModelScope.launch {
            try {
                repository.linkDeviceRecords(confirmation.session, confirmation.target.id, confirmation.recordIds)
                if (isCurrent(context)) mutable.value = mutable.value.copy(linking = false, selecting = false, selectedIds = emptySet(),
                    targetId = null, confirmation = null, message = HistoryMessage.Linked(confirmation.rows.size, confirmation.target.name))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (isCurrent(context)) mutable.value = mutable.value.copy(linking = false, confirmation = null,
                    message = HistoryMessage.LinkFailed)
            } finally { if (isCurrent(context)) mutable.value = mutable.value.copy(linking = false) }
        }
    }

    suspend fun shareRecord(id: Long, context: HistoryContext): Pair<RecordedSwim, SwimCompetition>? {
        if (!isCurrent(context)) return null
        val (swim, competition) = repository.load(id, context) ?: return null
        return if (isCurrent(context)) swim to competition else null
    }

    fun delete(id: Long) {
        val state = mutable.value
        val context = state.context ?: return
        if (!isCurrent(context) || state.selecting || state.choosingAthlete || id in state.deletingIds || state.groups.none { group -> group.swims.any { it.id == id } }) return
        mutable.value = state.copy(deletingIds = state.deletingIds + id, actionError = false)
        viewModelScope.launch {
            var committed = false
            try { repository.delete(id, context); committed = true }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (isCurrent(context)) mutable.value = mutable.value.copy(actionError = true) }
            finally {
                // A successful row remains disabled until the storage Flow removes it.
                if (!committed && isCurrent(context)) mutable.value = mutable.value.copy(deletingIds = mutable.value.deletingIds - id)
            }
        }
    }
}
