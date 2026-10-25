package com.msafonov.swimresults.data.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class TimeComponents(
    val minutes: Long,
    val seconds: Long,
    val milliseconds: Long
) {
    fun toRankFormat(): String {
        val msecs = if (milliseconds > 100L) milliseconds.toString().substring(1) else milliseconds.toString().padStart(2,'0')
        val minutes = if (minutes > 0L) "$minutes:" else ""
        val secs = seconds.toString().padStart(2, '0')
        return "$minutes$secs.$msecs"
    }

    companion object {
        fun from(time: String): TimeComponents {
            val minutes = if (time.contains(':')) time.split(':')[0] else "00"
            val seconds = time.substringAfter(':').substringBefore('.')
            val milliSeconds = time.substringAfter(':').substringAfter('.')

            return TimeComponents(minutes.toLong(), seconds.toLong(), milliSeconds.toLong())
        }

        fun TimeComponents.toDuration(): Duration =
           if (this.minutes > 0L) {
                this.minutes.minutes + this.seconds.seconds + this.milliseconds.milliseconds
            } else this.seconds.seconds + this.milliseconds.milliseconds

        fun durationToComponents(duration: Duration) = TimeComponents(
            minutes = duration.inWholeMinutes,
            seconds = duration.inWholeSeconds % 60,
            milliseconds = duration.inWholeMilliseconds % 1000
        )

        fun calculateDiff(time1: TimeComponents, time2: TimeComponents): TimeComponents {
            val duration1 = time1.toDuration()
            val duration2 = time2.toDuration()

            val difference = if (duration1 > duration2) {
                duration1 - duration2
            } else {
                duration2 - duration1
            }

            return durationToComponents(difference)
        }
    }
}