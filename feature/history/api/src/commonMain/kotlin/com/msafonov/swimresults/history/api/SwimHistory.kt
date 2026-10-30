package com.msafonov.swimresults.history.api

import com.msafonov.swimresults.data.model.*
import kotlinx.coroutines.flow.Flow

data class NewSwimRecord(
    val competitionName: String,
    val swimDate: String,
    val distanceMeters: Int,
    val style: SwimStyle,
    val gender: Swimmer,
    val pool: PoolType,
    val time: String
)

interface SwimHistory {
    val history: Flow<List<SwimCompetitionGroup>>
    val competitions: Flow<List<SwimCompetition>>
    suspend fun save(input: NewSwimRecord)
    suspend fun load(id: Long): Pair<RecordedSwim, SwimCompetition>?
    suspend fun update(id: Long, input: NewSwimRecord, selectedCompetitionId: Long?)
    suspend fun delete(id: Long)
}

fun parseTimeCentiseconds(raw: String): Long? {
    val text = raw.trim()
    val minuteParts = text.split(':')
    if (minuteParts.size !in 1..2) return null
    val secondParts = minuteParts.last().split('.')
    if (secondParts.size !in 1..2) return null
    val minutes = if (minuteParts.size == 2) minuteParts[0].toLongOrNull() ?: return null else 0L
    val seconds = secondParts[0].toLongOrNull() ?: return null
    val fraction = if (secondParts.size == 2) secondParts[1] else "00"
    if (fraction.isEmpty() || fraction.length > 2 || !fraction.all(Char::isDigit)) return null
    if (seconds !in 0L..59L || minutes !in 0L..99L) return null
    val centiseconds = fraction.padEnd(2, '0').toLongOrNull() ?: return null
    return minutes * 6000 + seconds * 100 + centiseconds
}

