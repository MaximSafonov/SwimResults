package com.msafonov.swimresults.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.data.model.CheckedResult
import com.msafonov.swimresults.data.repository.StopWatchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class StopWatchViewModel(
    private val repository: StopWatchRepository
): ViewModel() {
    private var pausedTime = 0L
    private var timerJob: Job? = null


    var isRunning by mutableStateOf(false)
    var elapsedTime by mutableStateOf(0L)
    val laps = mutableStateListOf<Long>()

    fun start() {
        if (isRunning) return
        isRunning = true
        elapsedTime = pausedTime
        timerJob = viewModelScope.launch {
            while (isRunning) {
                elapsedTime += 10
                delay(10)
            }
        }
    }

    fun pause() {
        isRunning = false
        pausedTime = elapsedTime
        timerJob?.cancel()
    }

    fun recordLap() {
        laps.add(0, elapsedTime)
    }

    fun reset() {
        pause()
        elapsedTime = 0L
        pausedTime = 0L
        laps.clear()
    }

    fun setTime() = repository.setTime(formatTime(elapsedTime))

    fun formatTime(ms: Long): String {
        val minutes = (ms / 60_000).toString().padStart(2, '0')
        val seconds = ((ms % 60_000) / 1000).toString().padStart(2, '0')
        val milliseconds = ((ms % 1000) / 10).toString().padStart(2, '0')
        return "$minutes:$seconds.$milliseconds"
    }
}