package com.msafonov.swimresults.data.repository

import com.msafonov.swimresults.data.local.dao.SwimHistoryDao
import com.msafonov.swimresults.data.local.entity.SwimRecordEntity
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.RankTableType
import com.msafonov.swimresults.data.model.RecordedSwim
import com.msafonov.swimresults.data.model.SwimCompetition
import com.msafonov.swimresults.data.model.SwimCompetitionGroup
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.model.SwimmingRank
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

data class NewSwimRecord(
    val competitionName: String,
    val swimDate: String,
    val distanceMeters: Int,
    val style: SwimStyle,
    val gender: Swimmer,
    val pool: PoolType,
    val time: String
)

@Single
class SwimHistoryRepository(
    private val dao: SwimHistoryDao,
    private val checkResultRepository: CheckResultRepository
) {
    val history = dao.observeHistory().map { groups ->
        groups.map { group ->
            SwimCompetitionGroup(
                competition = SwimCompetition(
                    group.competition.id, group.competition.name, group.competition.date
                ),
                swims = group.swims.sortedWith(compareByDescending<SwimRecordEntity> { it.swimDate }
                    .thenBy { it.distanceMeters }).mapNotNull(::toRecordedSwim)
            )
        }.filter { it.swims.isNotEmpty() }
    }

    val competitions = dao.observeCompetitions().map { rows ->
        rows.map { SwimCompetition(it.id, it.name, it.date) }
    }

    suspend fun save(input: NewSwimRecord) {
        dao.insertSwimWithCompetition(
            competitionName = input.competitionName.trim(),
            competitionDate = input.swimDate,
            swim = prepareSwim(input)
        )
    }

    suspend fun load(id: Long): Pair<RecordedSwim, SwimCompetition>? {
        val row = dao.getSwim(id) ?: return null
        val swim = checkNotNull(toRecordedSwim(row)) { "Unsupported stored swim" }
        val competition = checkNotNull(dao.getCompetition(row.competitionId))
        return swim to SwimCompetition(competition.id, competition.name, competition.date)
    }

    suspend fun update(id: Long, input: NewSwimRecord, selectedCompetitionId: Long?) {
        val original = dao.getSwim(id) ?: throw NoSuchElementException("Swim no longer exists")
        val updated = prepareSwim(input, original).copy(id = id)
        if (dao.updateSwimWithCompetition(
                updated, selectedCompetitionId, input.competitionName.trim(), input.swimDate
            ) != 1) {
            throw NoSuchElementException("Swim no longer exists")
        }
    }

    suspend fun delete(id: Long) {
        if (dao.deleteSwim(id) != 1) throw NoSuchElementException("Swim no longer exists")
    }

    private suspend fun prepareSwim(input: NewSwimRecord, original: SwimRecordEntity? = null): SwimRecordEntity {
        require(input.competitionName.isNotBlank())
        require(input.distanceMeters > 0 && input.time.isNotBlank())
        kotlinx.datetime.LocalDate.parse(input.swimDate)
        val centiseconds = requireNotNull(parseTimeCentiseconds(input.time)) { "Invalid swimming time" }
        val poolLength = if (input.pool == PoolType.Short) 25 else 50
        val athleticFieldsUnchanged = original != null &&
            original.distanceMeters == input.distanceMeters && original.style == input.style.name &&
            original.gender == input.gender.name && original.poolLengthMeters == poolLength &&
            original.timeCentiseconds == centiseconds
        val qualification = if (athleticFieldsUnchanged) original?.qualification else {
            val tableType = when (input.pool) {
                PoolType.Short -> when (input.gender) {
                    Swimmer.Men -> RankTableType.ShortWaterMan
                    Swimmer.Women -> RankTableType.ShortWaterWomen
                }
                PoolType.Long -> when (input.gender) {
                    Swimmer.Men -> RankTableType.LongWaterMan
                    Swimmer.Women -> RankTableType.LongWaterWomen
                }
            }
            try {
                val events = checkResultRepository.rankTable(tableType)
                checkResultRepository.checkResult(
                    events, input.distanceMeters, input.style, checkResultTime(centiseconds)
                )?.currentRank?.name
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
        return SwimRecordEntity(
            competitionId = original?.competitionId ?: 0L,
            swimDate = input.swimDate,
            distanceMeters = input.distanceMeters,
            style = input.style.name,
            gender = input.gender.name,
            poolLengthMeters = poolLength,
            timeCentiseconds = centiseconds,
            qualification = qualification
        )
    }

    private fun toRecordedSwim(row: SwimRecordEntity): RecordedSwim? {
        val style = runCatching { SwimStyle.valueOf(row.style) }.getOrNull() ?: return null
        val gender = runCatching { Swimmer.valueOf(row.gender) }.getOrNull() ?: return null
        return RecordedSwim(
            row.id, row.competitionId, row.swimDate, row.distanceMeters, style, gender,
            row.poolLengthMeters, row.timeCentiseconds,
            row.qualification?.let { runCatching { SwimmingRank.valueOf(it) }.getOrNull() }
        )
    }

    companion object {
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

        private fun checkResultTime(centiseconds: Long): String {
            val minutes = centiseconds / 6000
            val seconds = ((centiseconds % 6000) / 100).toString().padStart(2, '0')
            val fraction = (centiseconds % 100).toString().padStart(2, '0')
            return "$minutes:$seconds.$fraction"
        }
    }
}
