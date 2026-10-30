package com.msafonov.swimresults.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.data.storage.StopWatchStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class StopWatchViewModel(
    private val repository: StopWatchStorage
): ViewModel() {
       private var timerJob: Job? = null

    var isRunning by mutableStateOf(false)
    var elapsedTime by mutableStateOf(0L)
        private set
    val laps = mutableStateListOf<Long>()

    private var pausedDuration = Duration.ZERO
    private val timeSource = TimeSource.Monotonic

    fun start() {
        if (isRunning) return
        isRunning = true

        val startTimeMark = timeSource.markNow()

        timerJob = viewModelScope.launch {
            while (isRunning) {
                val currentDuration = startTimeMark.elapsedNow() + pausedDuration
                elapsedTime = currentDuration.inWholeMilliseconds
                delay(10)
            }
        }
    }

    fun pause() {
        isRunning = false
        pausedDuration = elapsedTime.milliseconds
        timerJob?.cancel()
        timerJob?.cancel()
    }

    fun recordLap() {
        laps.add(0, elapsedTime)
    }

    fun reset() {
        isRunning = false
        timerJob?.cancel()
        pausedDuration = Duration.ZERO
        elapsedTime = 0L
    }

    fun setTime() = repository.setTime(formatTime(elapsedTime))

    fun formatTime(ms: Long): String {
        val minutes = (ms / 60_000).toString().padStart(2, '0')
        val seconds = ((ms % 60_000) / 1000).toString().padStart(2, '0')
        val milliseconds = ((ms % 1000) / 10).toString().padStart(2, '0')
        return "$minutes:$seconds.$milliseconds"
    }
}