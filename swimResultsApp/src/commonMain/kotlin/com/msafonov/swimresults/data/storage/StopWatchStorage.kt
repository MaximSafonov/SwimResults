package com.msafonov.swimresults.data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow


interface StopWatchStorage {
    val stopTime: Flow<String?>
    fun setTime(time: String)
    fun dropTime()
}


class InMemoryStopWatchStorage: StopWatchStorage {
    override val stopTime: MutableStateFlow<String?> = MutableStateFlow(null)
    override fun dropTime() {
        stopTime.value = null
    }

    override fun setTime(time: String) {
        stopTime.value = time
    }
}