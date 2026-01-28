package com.msafonov.swimresults.data.storage

import com.msafonov.swimresults.data.model.SwimEvent
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.TimeComponents
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

object Converter {
    fun timeToDuration(components: TimeComponents): Duration {
        return components.minutes.minutes + components.seconds.seconds + components.milliseconds.nanoseconds
    }

    fun durationToComponents(duration: Duration): TimeComponents {
        val totalSeconds = duration.inWholeSeconds
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val nanoseconds = duration.inWholeNanoseconds % 1_000_000_000

        return TimeComponents(minutes, seconds, nanoseconds)
    }

    fun calculateDiff(time1: TimeComponents, time2: TimeComponents) {
        // Convert to durations
        val duration1 = timeToDuration(time1)
        val duration2 = timeToDuration(time2)

        // Calculate difference
        val difference = if (duration1 > duration2) {
            duration1 - duration2
        } else {
            duration2 - duration1
        }

        // Convert back to components
        val diffComponents = durationToComponents(difference)
    }

    fun convertStrToRanks(str: String) {
        val stringList = str.split("\\s+".toRegex()).mapIndexed { index, string ->
            Pair<SwimmingRank, String>(SwimmingRank.entries[index], string)
        }
    }

    // Alternative: Parse from a structured text format
    fun parseFromText(text: String): List<SwimEvent> {
        val events = mutableListOf<SwimEvent>()
        val lines = text.replace(',', ' ').split("\\s+".toRegex()).chunked(11)

        lines.forEach { line ->
            parseLine(line)?.let { events.add(it) }
        }

        return events
    }

    private fun parseLine(line: List<String>): SwimEvent? {
        if (line.size < SwimmingRank.entries.size) return null

        val distance = line[0].toIntOrNull() ?: return null
        val style = when (line[1].lowercase()) {
            "баттерфляй" -> SwimStyle.BUTTERFLY
            "брасс" -> SwimStyle.BREASTSTROKE
            "вольный стиль" -> SwimStyle.FREESTYLE
            "на спине" -> SwimStyle.BACKSTROKE
            "комплекс" -> SwimStyle.MEDLEY
            else -> return null
        }

        val times = mutableMapOf<SwimmingRank, TimeComponents>().apply {
            put(SwimmingRank.ThirdJunior, TimeComponents.from(line[2]))
            put(SwimmingRank.SecondJunior, TimeComponents.from(line[3]))
            put(SwimmingRank.First, TimeComponents.from(line[4]))
            put(SwimmingRank.Third, TimeComponents.from(line[5]))
            put(SwimmingRank.Second, TimeComponents.from(line[6]))
            put(SwimmingRank.First, TimeComponents.from(line[7]))
            put(SwimmingRank.CandidateMasterOfSports, TimeComponents.from(line[8]))
            put(SwimmingRank.MasterOfSports, TimeComponents.from(line[9]))
            put(SwimmingRank.InternationalMasterOfSports, TimeComponents.from(line[10]))
        }

        return SwimEvent(distance, style, times)
    }
}

//    59.05	49.55	39.55	32.55	30.55	27.85	26.55	25.75	24.13