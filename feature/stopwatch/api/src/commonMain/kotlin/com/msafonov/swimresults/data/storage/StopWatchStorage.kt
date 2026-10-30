package com.msafonov.swimresults.data.storage

import kotlinx.coroutines.flow.Flow

interface StopWatchStorage {
    val stopTime: Flow<String?>
    fun setTime(time: String)
    fun dropTime()
}
