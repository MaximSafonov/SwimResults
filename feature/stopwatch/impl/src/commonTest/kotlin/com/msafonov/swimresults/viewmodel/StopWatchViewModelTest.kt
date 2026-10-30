package com.msafonov.swimresults.viewmodel

import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.data.storage.InMemoryStopWatchStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class StopWatchViewModelTest {
    @Test
    fun formatsTime() {
        val model = StopWatchViewModel(InMemoryStopWatchStorage())
        assertEquals("00:00.00", model.formatTime(0))
        assertEquals("01:02.34", model.formatTime(62_349))
        assertEquals("100:00.00", model.formatTime(6_000_000))
    }

    @Test
    fun lapResetAndStoragePreserveCurrentBehavior() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val storage = InMemoryStopWatchStorage()
        val model = StopWatchViewModel(storage)
        try {
            model.recordLap()
            assertEquals(listOf(0L), model.laps.toList())
            model.start()
            runCurrent()
            model.pause()
            model.reset()
            assertFalse(model.isRunning)
            assertEquals(0L, model.elapsedTime)
            assertEquals(listOf(0L), model.laps.toList())
            model.setTime()
            assertEquals("00:00.00", storage.stopTime.value)
        } finally {
            model.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }
}
