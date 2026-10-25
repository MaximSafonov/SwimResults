package com.msafonov.swimresults.viewmodel

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Converts the app's legacy hundredths-as-milliseconds representation for presentation only. */
internal object ResultDetailsTime {
    fun inputCentiseconds(value: String): Long? {
        val match = INPUT.matchEntire(value) ?: return null
        val minutesText = match.groupValues[1]
        val secondsText = match.groupValues[2]
        val fractionText = match.groupValues[3]
        if (minutesText.isNotEmpty() && secondsText.length != 2) return null
        val minutes = if (minutesText.isEmpty()) 0L else minutesText.toLongOrNull() ?: return null
        val seconds = secondsText.toLongOrNull() ?: return null
        if (seconds > 59L) return null
        val fraction = fractionText.toLongOrNull() ?: return null
        if (minutes > Long.MAX_VALUE / 6_000) return null
        val base = minutes * 6_000
        val remainder = seconds * 100 + fraction
        if (base > Long.MAX_VALUE - remainder) return null
        return base + remainder
    }

    fun standardCentiseconds(duration: Duration): Long? {
        if (!duration.isFinite() || duration < Duration.ZERO) return null
        val millis = duration.inWholeMilliseconds
        if (duration != millis.milliseconds) return null
        val fraction = millis % 1_000
        if (fraction !in 0..99) return null
        val seconds = duration.inWholeSeconds
        if (seconds > (Long.MAX_VALUE - fraction) / 100) return null
        return seconds * 100 + fraction
    }

    fun difference(result: Long, standard: Long): Long? =
        if (standard < 0 || result < 0) null else result - standard

    fun formatDifference(centiseconds: Long): String {
        val sign = when {
            centiseconds > 0 -> "+"
            centiseconds < 0 -> "−"
            else -> ""
        }
        val seconds = centiseconds / 100
        val fraction = kotlin.math.abs(centiseconds % 100).toString().padStart(2, '0')
        return "$sign${kotlin.math.abs(seconds)}.$fraction"
    }

    private val INPUT = Regex("^(?:(\\d+):)?(\\d{1,2})\\.(\\d{2})$")
}
