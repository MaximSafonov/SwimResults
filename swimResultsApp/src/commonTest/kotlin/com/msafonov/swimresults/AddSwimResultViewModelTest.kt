package com.msafonov.swimresults

import com.msafonov.swimresults.data.model.Constants
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.repository.SwimHistoryRepository
import com.msafonov.swimresults.viewmodel.AddSwimResultUiState
import androidx.lifecycle.ViewModelStore
import com.msafonov.swimresults.viewmodel.AddSwimResultViewModel
import com.msafonov.swimresults.viewmodel.AddSwimResultError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AddSwimResultViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val store = ViewModelStore()

    @BeforeTest
    fun setUp() { Dispatchers.setMain(StandardTestDispatcher(scheduler)) }

    @AfterTest
    fun tearDown() {
        store.clear()
        scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun viewModel(dao: InMemoryHistoryDao, key: String = "form") =
        AddSwimResultViewModel(historyTestRepository(dao)).also { store.put(key, it) }

    @Test
    fun availableDistanceChoicesFollowExistingDomainRules() {
        val state = AddSwimResultUiState()
        assertEquals(Constants.FREESTYLE_LIST, state.distances)
        assertEquals(Constants.MEDLEY_LIST_SHORT,
            state.copy(style = SwimStyle.MEDLEY).distances)
        assertEquals(Constants.MEDLEY_LIST_LONG,
            state.copy(style = SwimStyle.MEDLEY, pool = PoolType.Long).distances)
    }

    @Test
    fun newFormCannotSaveUntilRequiredFieldsAndTimeAreValid() {
        val valid = AddSwimResultUiState(
            newCompetitionName = "Городские соревнования",
            swimDate = "2026-06-13",
            swimmer = Swimmer.Women,
            time = "1:23.45"
        )
        assertTrue(valid.canSave)
        assertFalse(valid.copy(time = "1:60.00").canSave)
        assertFalse(valid.copy(swimDate = "12.06.2026").canSave)
    }

    @Test
    fun formattedTimerInputIsValidAndParsesToCentiseconds() {
        val state = AddSwimResultUiState(
            newCompetitionName = "Городские соревнования",
            swimDate = "2026-06-13",
            time = "1:23.45"
        )

        assertTrue(state.canSave)
        assertEquals(8_345L, SwimHistoryRepository.parseTimeCentiseconds(state.time))
    }

    @Test
    fun editPrefillsOnceAndCompetitionEmissionsDoNotOverwriteInput() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val id = seedHistory(dao)
        val vm = viewModel(dao)
        assertFalse(vm.uiState.value.canSave)
        vm.initialize(id)
        scheduler.runCurrent()
        val loaded = vm.uiState.value
        assertEquals(id, loaded.recordId)
        assertEquals(dao.getSwim(id)!!.competitionId, loaded.selectedCompetitionId)
        assertEquals("2026-06-13", loaded.swimDate)
        assertEquals("1:23.45", loaded.time)
        assertEquals(100, loaded.distance)
        assertEquals(SwimStyle.FREESTYLE, loaded.style)
        assertEquals(Swimmer.Women, loaded.swimmer)
        assertEquals(PoolType.Short, loaded.pool)
        vm.setTime("1:25.01")
        vm.setSwimDate("2026-06-15")
        dao.emitChanges()
        vm.initialize(id)
        scheduler.runCurrent()
        assertEquals("1:25.01", vm.uiState.value.time)
        assertEquals("2026-06-15", vm.uiState.value.swimDate)
        val fresh = viewModel(dao, "fresh-add")
        fresh.initialize(null)
        scheduler.runCurrent()
        assertNull(fresh.uiState.value.recordId)
        assertEquals("", fresh.uiState.value.time)
        assertNull(fresh.uiState.value.selectedCompetitionId)
    }

    @Test
    fun saveUpdatesSameIdOnceAndKeepsEditedDateSeparateFromCompetitionDate() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val id = seedHistory(dao)
        val vm = viewModel(dao)
        vm.initialize(id)
        scheduler.runCurrent()
        vm.setSwimDate("2026-06-16")
        val gate = CompletableDeferred<Unit>()
        dao.beforeWrite = { gate.await() }
        var callbacks = 0
        vm.save { callbacks++ }
        vm.save { callbacks++ }
        scheduler.runCurrent()
        assertTrue(vm.uiState.value.saving)
        vm.setTime("10.00")
        gate.complete(Unit)
        scheduler.runCurrent()
        vm.save { callbacks++ }
        scheduler.runCurrent()
        assertEquals(1, callbacks)
        assertEquals(1, dao.updateCalls)
        assertEquals(listOf(id), dao.swims.map { it.id })
        assertEquals("2026-06-16", dao.getSwim(id)!!.swimDate)
        assertEquals("2026-06-12", dao.competitions.single().date)
        assertEquals(8_345L, dao.getSwim(id)!!.timeCentiseconds)
        assertTrue(vm.uiState.value.saved)
    }

    @Test
    fun missingLoadFailureAndStaleSaveNeverBecomeAdd() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val missing = viewModel(dao, "missing")
        missing.initialize(77)
        scheduler.runCurrent()
        assertEquals(AddSwimResultError.NotFound, missing.uiState.value.error)
        assertFalse(missing.uiState.value.canSave)
        dao.getFailure = IllegalStateException("read failed")
        val failed = viewModel(dao, "failed")
        failed.initialize(78)
        scheduler.runCurrent()
        assertEquals(AddSwimResultError.LoadRecord, failed.uiState.value.error)
        assertFalse(failed.uiState.value.canSave)
        dao.getFailure = null
        val id = seedHistory(dao)
        val stale = viewModel(dao, "stale")
        stale.initialize(id)
        scheduler.runCurrent()
        dao.swims.clear()
        dao.emitChanges()
        stale.save { error("Must not navigate after failed save") }
        scheduler.runCurrent()
        assertEquals(AddSwimResultError.NotFound, stale.uiState.value.error)
        assertFalse(stale.uiState.value.canSave)
        assertTrue(dao.swims.isEmpty())
    }

    @Test
    fun writeErrorPreservesInputAndCancellationDoesNotReportSaveFailure() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val id = seedHistory(dao)
        val vm = viewModel(dao)
        vm.initialize(id)
        scheduler.runCurrent()
        vm.setTime("1:25.01")
        dao.beforeWrite = { throw IllegalStateException("write failed") }
        vm.save { error("Must not navigate") }
        scheduler.runCurrent()
        assertEquals(AddSwimResultError.SaveFailure, vm.uiState.value.error)
        assertEquals("1:25.01", vm.uiState.value.time)
        assertTrue(vm.uiState.value.canSave)
        dao.beforeWrite = { throw CancellationException("cancel") }
        vm.save { error("Must not navigate on cancellation") }
        scheduler.runCurrent()
        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.saving)
        assertFalse(vm.uiState.value.saved)
        assertEquals(8_345L, dao.getSwim(id)!!.timeCentiseconds)
    }

    @Test
    fun clearingEntryWhileSavingCancelsWriteAndNavigation() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val id = seedHistory(dao)
        val vm = viewModel(dao)
        vm.initialize(id)
        scheduler.runCurrent()
        vm.setTime("1:25.01")
        val gate = CompletableDeferred<Unit>()
        dao.beforeWrite = { gate.await() }
        var callbacks = 0
        vm.save { callbacks++ }
        scheduler.runCurrent()
        store.clear()
        scheduler.runCurrent()
        gate.complete(Unit)
        scheduler.runCurrent()
        assertEquals(0, callbacks)
        assertEquals(8_345L, dao.getSwim(id)!!.timeCentiseconds)
    }
}
