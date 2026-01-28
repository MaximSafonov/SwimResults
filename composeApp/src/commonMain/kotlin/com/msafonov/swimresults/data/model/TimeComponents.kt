package com.msafonov.swimresults.data.model

data class TimeComponents(val minutes: Long, val seconds: Long, val milliseconds: Long) {
    companion object {
        fun from(time: String): TimeComponents {
            val minutes = if (time.contains(':')) time.split(':')[0] else "00"
            val seconds = time.substringAfter(':').substringBefore('.')
            val milliSeconds = time.substringAfter(':').substringAfter('.')

            return TimeComponents(minutes.toLong(), seconds.toLong(), milliSeconds.toLong())
        }
    }
}