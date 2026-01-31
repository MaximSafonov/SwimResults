package com.msafonov.swimresults.data.model

import com.msafonov.swimresults.log
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class TimeComponents(
    val minutes: Long,
    val seconds: Long,
    val milliseconds: Long
) {
    override fun toString(): String {
        val msecs = if (milliseconds > 100L) milliseconds.toString().substring(1) else milliseconds.toString()
        val minutes = if (minutes > 0L) "$minutes мин, " else ""
        val seconds = if (seconds > 0L) "$seconds сек, " else ""
        val ms = if (milliseconds > 0L) "$msecs сотых" else ""
        return "$minutes$seconds$ms"
    }

    companion object {
        fun from(time: String): TimeComponents {
            val minutes = if (time.contains(':')) time.split(':')[0] else "00"
            val seconds = time.substringAfter(':').substringBefore('.')
            val milliSeconds = time.substringAfter(':').substringAfter('.')

            return TimeComponents(minutes.toLong(), seconds.toLong(), milliSeconds.toLong())
        }

        fun TimeComponents.toDuration(): Duration {
            val minutes = if (this.minutes > 0L) this.minutes.minutes else null
            log(
                "checkResultsFunc",
                ".toDuration: $this ${this.minutes.minutes} ${this.seconds.seconds} ${this.milliseconds.milliseconds}"
            )
            return if (this.minutes > 0L) {
                this.minutes.minutes + this.seconds.seconds + this.milliseconds.milliseconds
            }
            else this.seconds.seconds + this.milliseconds.milliseconds
        }

        fun durationToComponents(duration: Duration): TimeComponents {
//            val totalSeconds = duration.inWholeSeconds
//            val minutes = totalSeconds / 60
//            val seconds = totalSeconds % 60
            val nanos = duration.inWholeMilliseconds % 1_000

            val min = duration.inWholeMinutes
            val sec = duration.inWholeSeconds % 60
            val ms = (duration.inWholeMilliseconds % 1000)

            log("final", "$min $sec $ms")

            val result = duration.toComponents { minutes, seconds, nanoseconds ->
                log("checkResultsFunc", "nanos $nanos  ${duration.inWholeSeconds}")
                TimeComponents(minutes, seconds.toLong(), (nanoseconds % 1_000_000).toLong())
            }

//            val result = TimeComponents(minutes, seconds, nanoseconds)
            log("checkResultsFunc", "durationToComponents:${duration.absoluteValue} $result ")
            return TimeComponents(min, sec, ms)
        }

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