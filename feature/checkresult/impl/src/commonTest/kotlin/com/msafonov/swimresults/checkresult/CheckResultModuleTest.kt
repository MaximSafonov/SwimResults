package com.msafonov.swimresults.checkresult

import com.msafonov.swimresults.checkresult.api.CheckResult
import com.msafonov.swimresults.checkresult.di.checkResultModule
import com.msafonov.swimresults.data.storage.StopWatchStorage
import com.msafonov.swimresults.stopwatch.di.stopwatchModule
import androidx.lifecycle.ViewModelStore
import com.msafonov.swimresults.viewmodel.CheckResultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class CheckResultModuleTest {
    @Test
    fun viewModelConsumesSharedStopwatchHandoff() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val app = startKoin { modules(stopwatchModule, checkResultModule) }
        val store = ViewModelStore()
        try {
            val vm = app.koin.get<CheckResultViewModel>()
            store.put("screen", vm)
            vm.startStopwatchUpdates()
            app.koin.get<StopWatchStorage>().setTime("1:23.45")
            testScheduler.runCurrent()
            assertEquals("1:23.45", vm.uiState.value.timerInput.text)
            assertNull(app.koin.get<StopWatchStorage>().stopTime.first())
        } finally {
            store.clear()
            stopKoin()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun stopwatchHandoffUsesOneSharedInstanceAcrossFeatures() = runTest {
        val app = startKoin { modules(stopwatchModule, checkResultModule) }
        try {
            val stopwatch = app.koin.get<StopWatchStorage>()
            val checkResult = app.koin.get<CheckResult>()
            assertSame(checkResult, app.koin.get<CheckResult>())
            stopwatch.setTime("1:23.45")
            assertEquals("1:23.45", checkResult.stopWatchTime.first())
            checkResult.dropTime()
            assertNull(stopwatch.stopTime.first())
        } finally { stopKoin() }
    }
}
