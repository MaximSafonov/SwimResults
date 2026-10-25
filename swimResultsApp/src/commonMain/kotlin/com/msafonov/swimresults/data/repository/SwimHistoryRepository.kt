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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import com.msafonov.swimresults.auth.AccountSession
import com.msafonov.swimresults.data.model.HistoryLabel
import com.msafonov.swimresults.data.model.HistoryContext
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
    private val checkResultRepository: CheckResultRepository,
    val session: AccountSession,
    val profiles: ProfileRepository
) {
    private val deviceLocal = kotlinx.coroutines.flow.MutableStateFlow(false)

    fun showDeviceLocal(value: Boolean) { deviceLocal.value = value }

    val contexts = kotlinx.coroutines.flow.combine(session.state, profiles.profiles, deviceLocal) { snapshot, profile, local ->
        if (snapshot.accountId == null || local) HistoryContext(snapshot)
        else if (profile.session == snapshot && profile.activeAthlete != null) {
            val athlete = profile.activeAthlete
            HistoryContext(snapshot, athlete!!.id, HistoryLabel.AthleteName(athlete.name))
        } else HistoryContext(snapshot, label = HistoryLabel.SelectAthlete, available = false)
    }.distinctUntilChanged()

    suspend fun currentContext(): HistoryContext {
        val snapshot = session.current
        if (snapshot.accountId == null || deviceLocal.value) return HistoryContext(snapshot)
        val active = profiles.current(snapshot).activeAthlete
        return if (active == null) HistoryContext(snapshot, label = HistoryLabel.SelectAthlete, available = false)
        else HistoryContext(snapshot, active.id, HistoryLabel.AthleteName(active.name))
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val history = contexts.flatMapLatest { context -> history(context) }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val competitions = contexts.flatMapLatest { context -> competitions(context) }

    fun history(context: HistoryContext): kotlinx.coroutines.flow.Flow<List<SwimCompetitionGroup>> = kotlinx.coroutines.flow.flow {
        if (!context.available || !session.isCurrent(context.session)) { emit(emptyList()); return@flow }
        if (context.athleteId != null) session.guard(context.session) { profiles.requireOwned(context.session, context.athleteId) }
        dao.observeScopedHistory(context.athleteId).collect { groups ->
            if (session.isCurrent(context.session)) emit(groups.map { group ->
                SwimCompetitionGroup(SwimCompetition(group.competition.id, group.competition.name, group.competition.date),
                    group.swims.sortedWith(compareByDescending<SwimRecordEntity> { it.swimDate }.thenBy { it.distanceMeters })
                        .mapNotNull(::toRecordedSwim))
            }.filter { it.swims.isNotEmpty() })
        }
    }

    fun competitions(context: HistoryContext) = history(context).map { groups -> groups.map { it.competition } }

    private suspend fun validate(context: HistoryContext) {
        require(context.available) { "Choose an athlete" }
        if (context.athleteId != null) profiles.requireOwned(context.session, context.athleteId)
    }

    private suspend fun row(context: HistoryContext, id: Long): SwimRecordEntity? {
        validate(context)
        return dao.getSwim(id)?.takeIf { it.athleteId == context.athleteId }
    }

    suspend fun save(input: NewSwimRecord) = save(input, currentContext())

    suspend fun initialGender(context: HistoryContext): Swimmer? = context.athleteId?.let { profiles.load(context.session, it)?.sportsGender }

    suspend fun save(input: NewSwimRecord, context: HistoryContext) {
        // Qualification calculation can suspend; re-check the captured epoch at commit.
        val prepared = prepareSwim(input).copy(athleteId = context.athleteId)
        session.guard(context.session) {
            validate(context)
            dao.insertSwimWithCompetition(input.competitionName.trim(), input.swimDate, prepared)
        }
    }

    suspend fun load(id: Long) = load(id, currentContext())

    suspend fun load(id: Long, context: HistoryContext): Pair<RecordedSwim, SwimCompetition>? = session.guard(context.session) {
        val row = row(context, id) ?: return@guard null
        val swim = checkNotNull(toRecordedSwim(row)) { "Unsupported stored swim" }
        val competition = checkNotNull(dao.getCompetition(row.competitionId))
        swim to SwimCompetition(competition.id, competition.name, competition.date)
    }

    suspend fun update(id: Long, input: NewSwimRecord, selectedCompetitionId: Long?) = update(id, input, selectedCompetitionId, currentContext())

    suspend fun update(id: Long, input: NewSwimRecord, selectedCompetitionId: Long?, context: HistoryContext) {
        val original = session.guard(context.session) { row(context, id) } ?: throw NoSuchElementException("Swim is unavailable")
        val updated = prepareSwim(input, original).copy(id = id, athleteId = original.athleteId)
        session.guard(context.session) {
            check(row(context, id) == original) { "Swim changed" }
            if (selectedCompetitionId != null && selectedCompetitionId != original.competitionId) {
                check(dao.observeScopedHistory(context.athleteId).first().any { it.competition.id == selectedCompetitionId }) { "Competition is unavailable" }
            }
            if (dao.updateSwimWithCompetition(updated, selectedCompetitionId, input.competitionName.trim(), input.swimDate) != 1)
                throw NoSuchElementException("Swim is unavailable")
        }
    }

    suspend fun delete(id: Long) = delete(id, currentContext())

    suspend fun delete(id: Long, context: HistoryContext) = session.guard(context.session) {
        if (row(context, id) == null || dao.deleteSwim(id) != 1) throw NoSuchElementException("Swim is unavailable")
    }

    suspend fun linkDeviceRecords(snapshot: com.msafonov.swimresults.auth.SessionSnapshot, athleteId: Long, ids: List<Long>) = session.guard(snapshot) {
        val accountId = requireNotNull(snapshot.accountId) { "Sign in first" }
        dao.linkDeviceRecords(accountId, athleteId, ids)
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
            row.qualification?.let { runCatching { SwimmingRank.valueOf(it) }.getOrNull() }, row.athleteId
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
