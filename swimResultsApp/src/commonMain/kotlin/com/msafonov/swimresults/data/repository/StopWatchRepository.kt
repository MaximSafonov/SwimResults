package com.msafonov.swimresults.data.repository

import com.msafonov.swimresults.data.storage.StopWatchStorage
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Singleton

@Singleton
class StopWatchRepository(
    private val stopWatchStorage: StopWatchStorage
) {
    val stopTime: Flow<String?> = stopWatchStorage.stopTime
    fun setTime(time: String) = stopWatchStorage.setTime(time)
    fun dropTime() = stopWatchStorage.dropTime()
}