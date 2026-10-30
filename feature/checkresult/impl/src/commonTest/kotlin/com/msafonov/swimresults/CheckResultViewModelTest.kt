package com.msafonov.swimresults

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModelStore
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.RankTableType
import com.msafonov.swimresults.data.model.SwimEvent
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.repository.CheckResultRepository
import com.msafonov.swimresults.data.storage.RankTable
import com.msafonov.swimresults.data.storage.StopWatchStorage
import com.msafonov.swimresults.viewmodel.CheckResultViewModel
import com.msafonov.swimresults.viewmodel.StandardsStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CheckResultViewModelTest {
    private val main = TestCoroutineScheduler()
    private val loading = TestCoroutineScheduler()
    private val store = ViewModelStore()
    private val tables = FakeTables()
    private val stopwatch = FakeStopwatch()

    @BeforeTest
    fun setUp() { Dispatchers.setMain(StandardTestDispatcher(main)) }

    @AfterTest
    fun tearDown() {
        store.clear()
        main.runCurrent()
        loading.runCurrent()
        Dispatchers.resetMain()
    }

    private fun viewModel(): CheckResultViewModel = CheckResultViewModel(
        CheckResultRepository(tables, stopwatch)
    ).also {
        it.tableDispatcher = StandardTestDispatcher(loading)
        it.startStopwatchUpdates()
        store.put("screen", it)
    }

    private fun finishLoad() {
        main.runCurrent()
        loading.runCurrent()
        main.runCurrent()
    }

    @Test
    fun initialLoadingThenReadyAndRepeatedChoicesDoNotReadAgain() {
        val vm = viewModel()
        assertEquals(StandardsStatus.Loading, vm.uiState.value.status)
        assertFalse(vm.uiState.value.canCheckResult)
        finishLoad()
        assertEquals(StandardsStatus.Ready, vm.uiState.value.status)
        assertEquals(listOf(RankTableType.ShortWaterWomen), tables.reads)
        vm.setSwimmer(Swimmer.Women)
        vm.checkPool(PoolType.Short)
        vm.setSwimStyle(SwimStyle.FREESTYLE)
        vm.setDistance(50)
        vm.setSwimStyle(SwimStyle.BREASTSTROKE)
        vm.setDistance(200)
        vm.setDistance(999)
        finishLoad()
        assertEquals(200, vm.uiState.value.filters.distance)
        assertEquals(SwimStyle.BREASTSTROKE, vm.uiState.value.currentEvent?.style)
        assertEquals(1, tables.reads.size)
    }

    @Test
    fun keyChangeClearsPreviousGridAndCacheAvoidsDuplicateLookup() {
        val vm = viewModel()
        finishLoad()
        assertTrue(vm.uiState.value.standards.any { it.time != null })
        vm.setSwimmer(Swimmer.Men)
        assertEquals(StandardsStatus.Loading, vm.uiState.value.status)
        assertNull(vm.uiState.value.currentEvent)
        assertTrue(vm.uiState.value.standards.all { it.time == null })
        finishLoad()
        assertEquals(StandardsStatus.Ready, vm.uiState.value.status)
        vm.setSwimmer(Swimmer.Women)
        assertEquals(StandardsStatus.Ready, vm.uiState.value.status)
        finishLoad()
        assertEquals(listOf(RankTableType.ShortWaterWomen, RankTableType.ShortWaterMan), tables.reads)
    }

    @Test
    fun loadingProjectsLatestStyleAndDistance() {
        val vm = viewModel()
        main.runCurrent()
        vm.setDistance(800)
        vm.setSwimStyle(SwimStyle.MEDLEY)
        assertEquals(100, vm.uiState.value.filters.distance)
        vm.setDistance(400)
        finishLoad()
        assertEquals(400, vm.uiState.value.currentEvent?.distance)
        assertEquals(SwimStyle.MEDLEY, vm.uiState.value.currentEvent?.style)
        assertEquals(1, tables.reads.size)
        vm.checkPool(PoolType.Long)
        vm.setDistance(200)
        finishLoad()
        assertEquals(200, vm.uiState.value.currentEvent?.distance)
    }

    @Test
    fun cancelledQueuedReadCannotPublishOrBecomeError() {
        val vm = viewModel()
        main.runCurrent()
        vm.setSwimmer(Swimmer.Men)
        vm.checkPool(PoolType.Long)
        finishLoad()
        assertEquals(StandardsStatus.Ready, vm.uiState.value.status)
        assertEquals(listOf(RankTableType.LongWaterMan), tables.reads)
    }

    @Test
    fun selectionDuringNonSuspendingReadRejectsOldCompletion() {
        val vm = viewModel()
        tables.onRead = { key ->
            if (key == RankTableType.ShortWaterWomen) vm.setSwimmer(Swimmer.Men)
        }
        finishLoad()
        // A second queued load may need another turn of the independent loader.
        finishLoad()
        assertEquals(StandardsStatus.Ready, vm.uiState.value.status)
        assertEquals(Swimmer.Men, vm.uiState.value.filters.swimmer)
        assertEquals(40.seconds, vm.uiState.value.standards.single { it.rank == SwimmingRank.First }.time)
        assertEquals(listOf(RankTableType.ShortWaterWomen, RankTableType.ShortWaterMan), tables.reads)
    }

    @Test
    fun failureIsRetryableAndSelectionRemainsUsable() {
        tables.fail = true
        val vm = viewModel()
        finishLoad()
        assertEquals(StandardsStatus.Error, vm.uiState.value.status)
        assertTrue(vm.uiState.value.standards.all { it.time == null })
        assertFalse(vm.uiState.value.canCheckResult)
        vm.setSwimStyle(SwimStyle.MEDLEY)
        vm.setDistance(400)
        assertEquals(StandardsStatus.Error, vm.uiState.value.status)
        tables.fail = false
        vm.retry()
        vm.retry() // Loading ignores duplicate retry.
        assertEquals(StandardsStatus.Loading, vm.uiState.value.status)
        finishLoad()
        assertEquals(StandardsStatus.Ready, vm.uiState.value.status)
        assertEquals(400, vm.uiState.value.currentEvent?.distance)
        assertEquals(2, tables.reads.size)
    }

    @Test
    fun unavailableEventHasNoStaleValuesAndCannotBeChecked() {
        val vm = viewModel()
        finishLoad()
        tables.empty = true
        vm.setSwimmer(Swimmer.Men)
        finishLoad()
        assertEquals(StandardsStatus.Unavailable, vm.uiState.value.status)
        vm.onInputChanged(TextFieldValue("0:35.00"))
        vm.checkResult()
        assertFalse(vm.uiState.value.showResult)
        assertNull(vm.uiState.value.currentEvent)
        assertTrue(vm.uiState.value.standards.all { it.time == null })
    }

    @Test
    fun stopwatchIsConsumedOnceExpandsSectionAndInvalidatesOldResult() {
        val vm = viewModel()
        finishLoad()
        vm.onInputChanged(TextFieldValue("0:30.00"))
        vm.checkResult()
        assertTrue(vm.uiState.value.showResult)
        stopwatch.setTime("0:31.20")
        main.runCurrent()
        assertEquals("0:31.20", vm.uiState.value.timerInput.text)
        assertTrue(vm.uiState.value.resultExpanded)
        assertFalse(vm.uiState.value.showResult)
        assertNull(vm.uiState.value.result)
        assertEquals(1, stopwatch.drops)
        assertNull(stopwatch.stopTime.value)
        vm.setDistance(100)
        main.runCurrent()
        assertEquals("0:31.20", vm.uiState.value.timerInput.text)
        assertEquals(1, stopwatch.drops)
    }

    @Test
    fun resultUsesCurrentEventAndBecomesInvalidOnInputOrFilterChange() {
        val vm = viewModel()
        finishLoad()
        vm.checkResult()
        assertFalse(vm.uiState.value.showResult)
        vm.onInputChanged(TextFieldValue("0:35.00"))
        vm.checkResult()
        assertNotNull(vm.uiState.value.result)
        assertNull(vm.uiState.value.result?.currentRank) // Women's threshold is 30s.
        vm.setSwimmer(Swimmer.Men)
        assertFalse(vm.uiState.value.showResult)
        assertNull(vm.uiState.value.result)
        assertEquals("0:35.00", vm.uiState.value.timerInput.text)
        finishLoad()
        vm.checkResult()
        assertEquals(SwimmingRank.First, vm.uiState.value.result?.currentRank) // Men's threshold is 40s.
        vm.onInputChanged(TextFieldValue("0:36.00"))
        assertNull(vm.uiState.value.result)
        assertFalse(vm.uiState.value.showResult)
        vm.checkResult()
        vm.setDistance(100)
        assertNull(vm.uiState.value.result)
        assertFalse(vm.uiState.value.showResult)
    }

    @Test
    fun resultDetailsCaptureCheckedParametersAndDismissWithoutChangingSelection() {
        val vm = viewModel()
        finishLoad()
        vm.onInputChanged(TextFieldValue("0:29.00"))
        vm.checkResult()
        val details = vm.uiState.value.resultDetails
        assertNotNull(details)
        assertEquals(50, details.distance)
        assertEquals(SwimStyle.FREESTYLE, details.swimStyle)
        assertEquals(Swimmer.Women, details.swimmer)
        assertEquals(PoolType.Short, details.poolType)
        assertEquals("0:29.00", details.input)
        val reads = tables.reads.size
        vm.dismissResult()
        assertNull(vm.uiState.value.resultDetails)
        assertEquals(50, vm.uiState.value.filters.distance)
        assertEquals("0:29.00", vm.uiState.value.timerInput.text)
        assertEquals(reads, tables.reads.size)
    }

    @Test
    fun pausedBackStackViewModelCannotConsumeNextScreensStopwatchHandoff() {
        val old = viewModel()
        finishLoad()
        old.stopStopwatchUpdates()
        main.runCurrent()
        stopwatch.setTime("0:32.10")
        main.runCurrent()
        assertEquals("", old.uiState.value.timerInput.text)
        assertEquals(0, stopwatch.drops)
        // A newly resumed screen receives the value; the old screen remains unchanged.
        val next = CheckResultViewModel(CheckResultRepository(tables, stopwatch))
        next.tableDispatcher = StandardTestDispatcher(loading)
        store.put("next", next)
        next.startStopwatchUpdates()
        finishLoad()
        assertEquals("0:32.10", next.uiState.value.timerInput.text)
        assertEquals("", old.uiState.value.timerInput.text)
        assertEquals(1, stopwatch.drops)
        next.stopStopwatchUpdates()
        next.startStopwatchUpdates()
        main.runCurrent()
        assertEquals(1, stopwatch.drops)
    }

    @Test
    fun clearingViewModelCancelsPendingLoadAndStopwatchCollection() {
        val vm = viewModel()
        main.runCurrent()
        store.clear()
        stopwatch.setTime("0:30.00")
        finishLoad()
        assertTrue(tables.reads.isEmpty())
        assertEquals(0, stopwatch.drops)
        assertEquals(StandardsStatus.Loading, vm.uiState.value.status)
    }

    private class FakeTables : RankTable {
        val reads = mutableListOf<RankTableType>()
        var fail = false
        var empty = false
        var onRead: (RankTableType) -> Unit = {}
        override fun rankTable(type: RankTableType): List<SwimEvent> {
            reads += type
            onRead(type)
            if (fail) error("Test source failure")
            if (empty) return emptyList()
            val threshold = if (type == RankTableType.ShortWaterMan ||
                type == RankTableType.LongWaterMan) 40.seconds else 30.seconds
            return SwimStyle.entries.flatMap { style ->
                listOf(50, 100, 200, 400, 800, 1500).map { distance ->
                    SwimEvent(distance, style, mapOf(threshold to SwimmingRank.First))
                }
            }
        }
    }

    private class FakeStopwatch : StopWatchStorage {
        override val stopTime = MutableStateFlow<String?>(null)
        var drops = 0
        override fun setTime(time: String) { stopTime.value = time }
        override fun dropTime() { drops++; stopTime.value = null }
    }
}
