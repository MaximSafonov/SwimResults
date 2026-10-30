package com.msafonov.swimresults.stopwatch

import com.msafonov.swimresults.data.storage.StopWatchStorage
import com.msafonov.swimresults.stopwatch.di.stopwatchModule
import com.msafonov.swimresults.viewmodel.StopWatchViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class StopwatchModuleTest {
    @Test
    fun sharedInstancePublishesAndClearsTime() = runTest {
        val app = startKoin { modules(stopwatchModule) }
        try {
            val writer = app.koin.get<StopWatchStorage>()
            val reader = app.koin.get<StopWatchStorage>()
            assertSame(writer, reader)
            val first = app.koin.get<StopWatchViewModel>()
            val second = app.koin.get<StopWatchViewModel>()
            assertNotSame(first, second)
            first.setTime()
            assertEquals("00:00.00", reader.stopTime.first())
            reader.dropTime()
            second.setTime()
            assertEquals("00:00.00", writer.stopTime.first())
            writer.setTime("1:23.45")
            assertEquals("1:23.45", reader.stopTime.first())
            reader.dropTime()
            assertNull(writer.stopTime.first())
        } finally { stopKoin() }
    }
}
