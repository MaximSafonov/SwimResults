package com.msafonov.swimresults.data.storage

import com.msafonov.swimresults.data.model.SwimEvent
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.TimeComponents
import com.msafonov.swimresults.data.model.TimeComponents.Companion.toDuration
import kotlin.time.Duration

object Converter {

    fun convertStrToRanks(str: String) {
        val stringList = str.split("\\s+".toRegex()).mapIndexed { index, string ->
            Pair<SwimmingRank, String>(SwimmingRank.entries[index], string)
        }
    }

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
            "вольный" -> SwimStyle.FREESTYLE
            "спина" -> SwimStyle.BACKSTROKE
            "комплекс" -> SwimStyle.MEDLEY
            else -> return null
        }

        val times = mutableMapOf<Duration, SwimmingRank>().apply {
            put(TimeComponents.from(line[2]).toDuration(), SwimmingRank.ThirdJunior)
            put(TimeComponents.from(line[3]).toDuration(), SwimmingRank.SecondJunior)
            put(TimeComponents.from(line[4]).toDuration(), SwimmingRank.First)
            put(TimeComponents.from(line[5]).toDuration(), SwimmingRank.Third)
            put(TimeComponents.from(line[6]).toDuration(), SwimmingRank.Second)
            put(TimeComponents.from(line[7]).toDuration(), SwimmingRank.First)
            put(TimeComponents.from(line[8]).toDuration(), SwimmingRank.CandidateMasterOfSports)
            put(TimeComponents.from(line[9]).toDuration(), SwimmingRank.MasterOfSports)
            put(TimeComponents.from(line[10]).toDuration(), SwimmingRank.InternationalMasterOfSports)
        }

        return SwimEvent(distance, style, times)
    }
}