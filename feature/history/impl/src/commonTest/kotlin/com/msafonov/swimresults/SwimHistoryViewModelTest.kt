package com.msafonov.swimresults

import androidx.lifecycle.ViewModelStore
import com.msafonov.swimresults.data.model.RecordedSwim
import com.msafonov.swimresults.data.model.SwimCompetition
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.model.SwimmingRank
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

    private fun viewModel(dao: InMemoryHistoryDao) =
        SwimHistoryViewModel(historyTestRepository(dao)).also { store.put("history", it) }

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
    fun shareTextIncludesBothDatesExactTimeAndOnlySelectedRecord() {
        val competition = SwimCompetition(1, "Кубок города", "2026-06-12")
        val swim = RecordedSwim(2, 1, "2026-06-13", 100, SwimStyle.FREESTYLE,
            Swimmer.Women, 25, 8_345, null)
        val labels = HistoryShareLabels("Соревнование", "Дата соревнования", "Дата заплыва",
            "Дистанция", "Пол", "Бассейн", "Время", "Разряд", "м", "Юноши", "Девушки", "Без разряда")
        assertEquals(
            "Соревнование: Кубок города\nДата соревнования: 12.06.2026\nДата заплыва: 13.06.2026\n" +
                "Дистанция: 100 м, Вольный стиль\nПол: Девушки\nБассейн: 25 м\nВремя: 1:23.45\nРазряд: Без разряда",
            formatHistoryShareText(competition, swim, labels)
        )
        val qualified = formatHistoryShareText(competition, swim.copy(timeCentiseconds = 903,
            qualification = SwimmingRank.First, gender = Swimmer.Men, poolLengthMeters = 50), labels)
        assertTrue(qualified.contains("Время: 0:09.03"))
        assertTrue(qualified.contains("Пол: Юноши"))
        assertTrue(qualified.contains("Бассейн: 50 м"))
        assertTrue(qualified.contains("Разряд: ${SwimmingRank.First.text}"))
    }
}
