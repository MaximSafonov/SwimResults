package com.msafonov.swimresults

import androidx.lifecycle.ViewModelStore
import com.msafonov.swimresults.auth.AccountSession
import com.msafonov.swimresults.auth.AuthState
import com.msafonov.swimresults.data.local.dao.ProfileDao
import com.msafonov.swimresults.data.local.entity.AccountEntity
import com.msafonov.swimresults.data.local.entity.AthleteEntity
import com.msafonov.swimresults.data.repository.ProfileRepository
import com.msafonov.swimresults.data.repository.SwimHistoryRepository
import com.msafonov.swimresults.data.repository.CheckResultRepository
import com.msafonov.swimresults.data.storage.RankTableStorage
import com.msafonov.swimresults.data.storage.InMemoryStopWatchStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import com.msafonov.swimresults.data.model.RecordedSwim
import com.msafonov.swimresults.data.model.SwimCompetition
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.HistoryLabel
import com.msafonov.swimresults.viewmodel.HistoryMessage
import com.msafonov.swimresults.viewmodel.SwimHistoryViewModel
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertSame
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class SwimHistoryViewModelTest {
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

    private fun viewModel(dao: InMemoryHistoryDao): SwimHistoryViewModel {
        val repository = historyTestRepository(dao)
        return SwimHistoryViewModel(repository, repository.profiles).also { store.put("history", it) }
    }

    @Test
    fun deletePersistsAndFlowRemovesOnlyTargetThenLastGroup() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val first = seedHistory(dao)
        val second = seedHistory(dao)
        val vm = viewModel(dao)
        scheduler.runCurrent()
        val gate = CompletableDeferred<Unit>()
        dao.beforeWrite = { gate.await() }
        vm.delete(first)
        vm.delete(first)
        scheduler.runCurrent()
        assertEquals(setOf(first), vm.uiState.value.deletingIds)
        assertEquals(1, dao.deleteCalls)
        gate.complete(Unit)
        scheduler.runCurrent()
        assertEquals(listOf(second), vm.uiState.value.groups.single().swims.map { it.id })
        assertEquals(listOf(second), dao.swims.map { it.id })
        vm.delete(second)
        scheduler.runCurrent()
        assertTrue(vm.uiState.value.groups.isEmpty())
        assertTrue(dao.swims.isEmpty())
        assertTrue(dao.competitions.isNotEmpty())
        assertFalse(vm.uiState.value.actionError)
    }

    @Test
    fun deleteFailureKeepsGroupsAndCancellationIsNotActionError() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val id = seedHistory(dao)
        val vm = viewModel(dao)
        scheduler.runCurrent()
        val groups = vm.uiState.value.groups
        dao.beforeWrite = { throw IllegalStateException("storage failed") }
        vm.delete(id)
        scheduler.runCurrent()
        assertEquals(groups, vm.uiState.value.groups)
        assertTrue(vm.uiState.value.actionError)
        assertTrue(vm.uiState.value.deletingIds.isEmpty())
        dao.beforeWrite = { throw CancellationException("cancel") }
        vm.delete(id)
        scheduler.runCurrent()
        assertEquals(groups, vm.uiState.value.groups)
        assertFalse(vm.uiState.value.actionError)
        assertTrue(vm.uiState.value.deletingIds.isEmpty())
    }

    @Test
    fun linkingRequiresExplicitConfirmationAndCancelWritesNothing() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val id = seedHistory(dao)
        val session = com.msafonov.swimresults.auth.AccountSession()
        signedIn(session)
        val repository = historyTestRepository(dao, session)
        val profile = repository.profiles
        profile.current()
        val athlete = profile.save(session.current, null, "Первый", "", null)
        val another = profile.save(session.current, null, "Второй", "", null)
        repository.showDeviceLocal(true)
        val vm = SwimHistoryViewModel(repository, profile).also { store.put("history", it) }
        scheduler.runCurrent()
        vm.startSelection(); vm.toggle(id); vm.target(athlete); vm.prepareLink()
        assertEquals(listOf(id), vm.uiState.value.confirmation!!.recordIds)
        profile.select(session.current, another)
        scheduler.runCurrent()
        assertEquals(athlete, vm.uiState.value.confirmation!!.target.id)
        vm.dismissConfirmation(); vm.cancelSelection()
        assertEquals(null, dao.getSwim(id)!!.athleteId)
        vm.startSelection(); vm.toggle(id); vm.target(athlete); vm.prepareLink(); vm.confirmLink()
        scheduler.runCurrent()
        assertEquals(athlete, dao.getSwim(id)!!.athleteId)
        assertEquals(HistoryMessage.Linked(1, "Первый"), vm.uiState.value.message)
        assertFalse(vm.uiState.value.selecting)
        assertTrue(vm.uiState.value.groups.isEmpty())
    }

    @Test
    fun selectionLimitIsBoundedAndChangedConfirmedRowsRequireNewConfirmation() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        repeat(201) { seedHistory(dao) }
        val session = com.msafonov.swimresults.auth.AccountSession()
        signedIn(session)
        val repository = historyTestRepository(dao, session)
        repository.profiles.current()
        val target = repository.profiles.save(session.current, null, "Спортсмен", "", null)
        repository.showDeviceLocal(true)
        val vm = SwimHistoryViewModel(repository, repository.profiles).also { store.put("history", it) }
        scheduler.runCurrent()
        vm.startSelection()
        dao.swims.forEach { vm.toggle(it.id) }
        assertEquals(200, vm.uiState.value.selectedIds.size)
        assertEquals(HistoryMessage.SelectionLimit(200), vm.uiState.value.message)
        vm.target(target); vm.prepareLink()
        assertEquals(200, vm.uiState.value.confirmation!!.rows.size)
        val row = dao.swims.first()
        dao.updateSwim(row.copy(timeCentiseconds = row.timeCentiseconds + 1))
        scheduler.runCurrent()
        assertEquals(null, vm.uiState.value.confirmation)
        assertEquals(HistoryMessage.RecordsChanged, vm.uiState.value.message)
        assertTrue(dao.swims.all { it.athleteId == null })
    }

    @Test
    fun logoutInvalidatesConfirmationAndDeferredShare() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val id = seedHistory(dao)
        val session = com.msafonov.swimresults.auth.AccountSession()
        signedIn(session)
        val repository = historyTestRepository(dao, session)
        repository.profiles.current()
        val athlete = repository.profiles.save(session.current, null, "Первый", "", null)
        repository.showDeviceLocal(true)
        val vm = SwimHistoryViewModel(repository, repository.profiles).also { store.put("history", it) }
        scheduler.runCurrent()
        val context = vm.uiState.value.context!!
        vm.startSelection(); vm.toggle(id); vm.target(athlete); vm.prepareLink()
        session.change(com.msafonov.swimresults.auth.AuthState.Guest())
        vm.confirmLink()
        scheduler.runCurrent()
        assertEquals(null, dao.getSwim(id)!!.athleteId)
        assertEquals(null, vm.uiState.value.confirmation)
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
        assertEquals(null, vm.shareRecord(id, context))
    }

    @Test
    fun ensureAccountFailureIsHandledAndNextSessionCanLoadHistory() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val local = seedHistory(dao)
        val session = AccountSession()
        signedIn(session)
        val failing = FailingHistoryProfileDao(dao.profileDao)
        failing.failure.value = ProfileReadFailure(ProfileReadSource.EnsureAccount, IllegalStateException("account write failed"))
        val profiles = ProfileRepository(failing, session)
        val repository = scopedRepository(dao, profiles)
        val vm = SwimHistoryViewModel(repository, profiles).also { store.put("history", it) }
        scheduler.runCurrent() // An uncaught upstream launch failure fails this test.
        assertClearedLoadFailure(vm)
        failing.failure.value = null
        session.change(AuthState.Guest())
        scheduler.runCurrent()
        assertFalse(vm.uiState.value.error)
        assertEquals(session.current, vm.uiState.value.context!!.session)
        assertEquals(listOf(local), vm.uiState.value.groups.single().swims.map { it.id })
        assertTrue(vm.uiState.value.athletes.isEmpty())
    }

    @Test
    fun profileObserverFailuresClearConfirmationAndExplicitRetryRecovers() = runTest(scheduler) {
        for (source in listOf(ProfileReadSource.ObserveAccount, ProfileReadSource.ObserveAthletes)) {
            val dao = InMemoryHistoryDao()
            val local = seedHistory(dao)
            val session = AccountSession()
            signedIn(session)
            val failing = FailingHistoryProfileDao(dao.profileDao)
            val profiles = ProfileRepository(failing, session)
            profiles.current()
            val target = profiles.save(session.current, null, "Спортсмен", "", null)
            val repository = scopedRepository(dao, profiles)
            repository.showDeviceLocal(true)
            val vm = SwimHistoryViewModel(repository, profiles).also { store.put("history", it) }
            scheduler.runCurrent()
            val previousContext = vm.uiState.value.context!!
            vm.startSelection(); vm.toggle(local); vm.target(target); vm.prepareLink()
            assertEquals(listOf(local), vm.uiState.value.confirmation!!.recordIds)
            assertTrue(vm.uiState.value.athletes.isNotEmpty())
            failing.failure.value = ProfileReadFailure(source, IllegalStateException("profile observation failed"))
            scheduler.runCurrent()
            assertClearedLoadFailure(vm)
            assertEquals(null, vm.shareRecord(local, previousContext))
            vm.confirmLink()
            scheduler.runCurrent()
            assertEquals(null, dao.getSwim(local)!!.athleteId)
            failing.failure.value = null
            vm.deviceLocal(true) // A context tap cannot restart a failed cold Flow.
            scheduler.runCurrent()
            assertClearedLoadFailure(vm)
            vm.retry()
            scheduler.runCurrent()
            assertFalse(vm.uiState.value.error)
            assertEquals(session.current, vm.uiState.value.context!!.session)
            assertEquals(listOf(local), vm.uiState.value.groups.single().swims.map { it.id })
            assertEquals(listOf(target), vm.uiState.value.athletes.map { it.id })
            assertEquals(null, vm.uiState.value.confirmation)
            assertTrue(vm.uiState.value.selectedIds.isEmpty())
            store.clear()
            scheduler.runCurrent()
        }
    }

    @Test
    fun sessionChangeCancelsSuspendedProfileReadWithoutPublishingAnErrorForNewSession() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val local = seedHistory(dao)
        val session = AccountSession()
        signedIn(session)
        val failing = FailingHistoryProfileDao(dao.profileDao)
        failing.blockedAccountId = session.current.accountId
        failing.readGate = CompletableDeferred()
        val profiles = ProfileRepository(failing, session)
        val repository = scopedRepository(dao, profiles)
        val vm = SwimHistoryViewModel(repository, profiles).also { store.put("history", it) }
        scheduler.runCurrent()
        assertFalse(vm.uiState.value.error)
        assertEquals(0, failing.cancelledReads)
        session.change(AuthState.Guest())
        scheduler.runCurrent()
        assertTrue(failing.cancelledReads > 0)
        assertFalse(vm.uiState.value.error)
        assertFalse(vm.uiState.value.actionError)
        assertEquals(session.current, vm.uiState.value.context!!.session)
        assertEquals(listOf(local), vm.uiState.value.groups.single().swims.map { it.id })
        assertTrue(vm.uiState.value.athletes.isEmpty())
        assertEquals(null, vm.uiState.value.confirmation)
    }

    @Test
    fun repeatedAthleteAndDeviceLocalChoiceDoNotWriteCollectOrEmitAndKeepConfirmation() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val local = seedHistory(dao)
        val session = AccountSession()
        signedIn(session)
        val repository = historyTestRepository(dao, session)
        val profiles = repository.profiles
        profiles.current()
        val first = profiles.save(session.current, null, "Первый", "01.02.2010", null, SwimmingRank.First)
        val second = profiles.save(session.current, null, "Второй", "", null)
        repository.save(historyTestInput())
        val vm = SwimHistoryViewModel(repository, profiles).also { store.put("history", it) }
        scheduler.runCurrent()
        val emitted = mutableListOf<com.msafonov.swimresults.viewmodel.SwimHistoryUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(scheduler)) { vm.uiState.collect { emitted += it } }
        val loaded = vm.uiState.value
        val writes = dao.profileDao.activeWrites
        val collections = dao.profileDao.athleteCollections
        val emissions = emitted.size
        vm.selectAthlete(first); vm.deviceLocal(false); vm.retry()
        scheduler.runCurrent()
        assertSame(loaded, vm.uiState.value)
        assertEquals(writes, dao.profileDao.activeWrites)
        assertEquals(collections, dao.profileDao.athleteCollections)
        assertEquals(emissions, emitted.size)
        vm.selectAthlete(second); scheduler.runCurrent()
        assertEquals(second, profiles.current().activeAthleteId)
        assertEquals(second, vm.uiState.value.context!!.athleteId)
        assertTrue(vm.uiState.value.groups.isEmpty())
        assertEquals(listOf(first, second), vm.uiState.value.athletes.map { it.id })
        vm.selectAthlete(999); scheduler.runCurrent()
        assertEquals(second, vm.uiState.value.context!!.athleteId)
        vm.deviceLocal(true); scheduler.runCurrent()
        vm.startSelection(); vm.toggle(local); vm.target(first); vm.prepareLink()
        val confirmed = vm.uiState.value
        val localWrites = dao.profileDao.activeWrites
        val localCollections = dao.profileDao.athleteCollections
        val localEmissions = emitted.size
        vm.deviceLocal(true); scheduler.runCurrent()
        assertSame(confirmed, vm.uiState.value)
        assertEquals(localWrites, dao.profileDao.activeWrites)
        assertEquals(localCollections, dao.profileDao.athleteCollections)
        assertEquals(localEmissions, emitted.size)
        assertEquals(SwimmingRank.First, vm.uiState.value.confirmation!!.target.rank)
        profiles.save(session.current, first, "Первый", "03.04.2011", null, SwimmingRank.Second)
        scheduler.runCurrent()
        assertEquals(null, vm.uiState.value.confirmation)
        assertEquals(HistoryMessage.AthleteChanged, vm.uiState.value.message)
    }

    @Test
    fun selectionFailureCancellationAndLinkingKeepTheCurrentContext() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val local = seedHistory(dao)
        val session = AccountSession()
        signedIn(session)
        val repository = historyTestRepository(dao, session)
        val profiles = repository.profiles
        profiles.current()
        val first = profiles.save(session.current, null, "Первый", "", null)
        val second = profiles.save(session.current, null, "Второй", "", null)
        val vm = SwimHistoryViewModel(repository, profiles).also { store.put("history", it) }
        scheduler.runCurrent()
        dao.profileDao.beforeActiveWrite = { throw IllegalStateException("write failed") }
        vm.selectAthlete(second); scheduler.runCurrent()
        assertEquals(first, vm.uiState.value.context!!.athleteId)
        assertTrue(vm.uiState.value.actionError)
        assertFalse(vm.uiState.value.choosingAthlete)
        dao.profileDao.beforeActiveWrite = { throw CancellationException("cancel") }
        vm.selectAthlete(second); scheduler.runCurrent()
        assertEquals(first, vm.uiState.value.context!!.athleteId)
        assertFalse(vm.uiState.value.actionError)
        assertFalse(vm.uiState.value.choosingAthlete)
        dao.profileDao.beforeActiveWrite = {}
        vm.deviceLocal(true); scheduler.runCurrent()
        vm.startSelection(); vm.toggle(local); vm.target(first); vm.prepareLink()
        val gate = CompletableDeferred<Unit>()
        dao.beforeWrite = { gate.await() }
        vm.confirmLink(); scheduler.runCurrent()
        val linking = vm.uiState.value
        vm.selectAthlete(second); vm.deviceLocal(false)
        scheduler.runCurrent()
        assertSame(linking, vm.uiState.value)
        gate.complete(Unit); scheduler.runCurrent()
        session.change(AuthState.Guest())
        vm.selectAthlete(second); scheduler.runCurrent()
        assertEquals(null, vm.uiState.value.context!!.athleteId)
        assertTrue(vm.uiState.value.athletes.isEmpty())
    }

    private fun assertClearedLoadFailure(vm: SwimHistoryViewModel) {
        val state = vm.uiState.value
        assertTrue(state.error)
        assertFalse(state.loading)
        assertFalse(state.context!!.available)
        assertEquals(HistoryLabel.LoadError, state.context!!.label)
        assertTrue(state.groups.isEmpty())
        assertTrue(state.athletes.isEmpty())
        assertTrue(state.deletingIds.isEmpty())
        assertFalse(state.selecting)
        assertTrue(state.selectedIds.isEmpty())
        assertEquals(null, state.targetId)
        assertEquals(null, state.confirmation)
        assertFalse(state.linking)
    }

    private fun scopedRepository(dao: InMemoryHistoryDao, profiles: ProfileRepository) = SwimHistoryRepository(
        dao, CheckResultRepository(RankTableStorage(), InMemoryStopWatchStorage()), profiles.session, profiles
    )

    @Test
    fun shareLoadsLatestRequestedRecordWithoutIncludingOtherRows() = runTest(scheduler) {
        val dao = InMemoryHistoryDao()
        val selected = seedHistory(dao)
        val other = seedHistory(dao)
        val vm = viewModel(dao)
        scheduler.runCurrent()
        val context = vm.uiState.value.context!!
        val row = dao.getSwim(selected)!!
        dao.updateSwim(row.copy(timeCentiseconds = 903))
        scheduler.runCurrent()
        val loaded = vm.shareRecord(selected, context)!!
        assertEquals(selected, loaded.first.id)
        assertEquals(903L, loaded.first.timeCentiseconds)
        assertTrue(vm.uiState.value.groups.flatMap { it.swims }.any { it.id == other })
        val text = formatHistoryShareText(loaded.second, loaded.first)
        assertEquals(1, text.lines().count { it.startsWith("Время:") })
        assertTrue(text.contains("Время: 0:09.03"))
    }

    @Test
    fun shareTextIncludesBothDatesExactTimeAndOnlySelectedRecord() = runTest(scheduler) {
        val competition = SwimCompetition(1, "Кубок города & <финал> 100%", "2026-06-12")
        val swim = RecordedSwim(2, 1, "2026-06-13", 100, SwimStyle.FREESTYLE,
            Swimmer.Women, 25, 8_345, null)
        assertEquals(
            "Соревнование: Кубок города & <финал> 100%\nДата соревнования: 12.06.2026\nДата заплыва: 13.06.2026\n" +
                "Дистанция: 100 м, Вольный стиль\nПол: Девушки\nБассейн: 25 м\nВремя: 1:23.45\nРазряд: Без разряда",
            formatHistoryShareText(competition, swim)
        )
        val qualified = formatHistoryShareText(competition, swim.copy(timeCentiseconds = 903,
            qualification = SwimmingRank.First, gender = Swimmer.Men, poolLengthMeters = 50))
        assertTrue(qualified.contains("Время: 0:09.03"))
        assertTrue(qualified.contains("Пол: Юноши"))
        assertTrue(qualified.contains("Бассейн: 50 м"))
        assertTrue(qualified.contains("Разряд: I"))
    }
}

private enum class ProfileReadSource { EnsureAccount, ObserveAccount, ObserveAthletes }
private data class ProfileReadFailure(val source: ProfileReadSource, val error: Exception)

/** Failure injection remains in this test file; production DAOs and shared fixtures are unchanged. */
private class FailingHistoryProfileDao(private val delegate: InMemoryProfileDao) : ProfileDao() {
    val failure = MutableStateFlow<ProfileReadFailure?>(null)
    var blockedAccountId: String? = null
    var readGate: CompletableDeferred<Unit>? = null
    var cancelledReads = 0
        private set

    override fun observeAccount(accountId: String): Flow<AccountEntity?> = flow {
        if (accountId == blockedAccountId) {
            try { readGate?.await() }
            catch (cancelled: CancellationException) { cancelledReads++; throw cancelled }
        }
        emitAll(combine(delegate.observeAccount(accountId), failure) { value, failed ->
            if (failed?.source == ProfileReadSource.ObserveAccount) throw failed.error
            value
        })
    }

    override fun observeAthletes(accountId: String): Flow<List<AthleteEntity>> =
        combine(delegate.observeAthletes(accountId), failure) { value, failed ->
            if (failed?.source == ProfileReadSource.ObserveAthletes) throw failed.error
            value
        }

    override suspend fun ensureAccount(account: AccountEntity) {
        val failed = failure.value
        if (failed?.source == ProfileReadSource.EnsureAccount) throw failed.error
        super.ensureAccount(account)
    }

    override suspend fun getAccount(accountId: String) = delegate.getAccount(accountId)
    override suspend fun ownedAthlete(accountId: String, id: Long) = delegate.ownedAthlete(accountId, id)
    override suspend fun insertAccount(account: AccountEntity) = delegate.insertAccount(account)
    override suspend fun updateAccountName(accountId: String, name: String) = delegate.updateAccountName(accountId, name)
    override suspend fun insertAthlete(athlete: AthleteEntity) = delegate.insertAthlete(athlete)
    override suspend fun updateAthlete(accountId: String, id: Long, name: String, birthDate: String?, gender: String?, rank: String?) =
        delegate.updateAthlete(accountId, id, name, birthDate, gender, rank)
    override suspend fun updateActive(accountId: String, athleteId: Long) = delegate.updateActive(accountId, athleteId)
}
