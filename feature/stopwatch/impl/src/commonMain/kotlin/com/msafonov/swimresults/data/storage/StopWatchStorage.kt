package com.msafonov.swimresults.data.storage

import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryStopWatchStorage: StopWatchStorage {
    override val stopTime: MutableStateFlow<String?> = MutableStateFlow(null)
    override fun dropTime() {
        stopTime.value = null
    }

    override fun setTime(time: String) {
        stopTime.value = time
    }
}