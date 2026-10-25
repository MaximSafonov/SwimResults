package com.msafonov.swimresults.data.repository

import com.msafonov.swimresults.auth.AccountSession
import com.msafonov.swimresults.auth.SessionSnapshot
import com.msafonov.swimresults.data.local.dao.ProfileDao
import com.msafonov.swimresults.data.local.entity.AccountEntity
import com.msafonov.swimresults.data.local.entity.AthleteEntity
import com.msafonov.swimresults.data.model.AthleteBirthDate
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.Athlete
import com.msafonov.swimresults.data.model.Profile
import com.msafonov.swimresults.data.model.Swimmer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single

@Single
class ProfileRepository(private val dao: ProfileDao, val session: AccountSession) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val profiles = session.state.flatMapLatest { snapshot -> flow {
        // Drop the previous account before any database suspension.
        emit(Profile(snapshot))
        val principal = snapshot.principal ?: return@flow
        try {
            session.guard(snapshot) {
                dao.ensureAccount(AccountEntity(principal.accountId, "VK", principal.userId, principal.displayName))
            }
        } catch (_: com.msafonov.swimresults.auth.StaleSessionException) { return@flow }
        combine(dao.observeAccount(principal.accountId), dao.observeAthletes(principal.accountId)) { account, athletes ->
            Profile(snapshot, athletes.map(::toAthlete), account?.activeAthleteId)
        }.collect { if (session.isCurrent(snapshot)) emit(it) }
    } }

    suspend fun current(snapshot: SessionSnapshot = session.current): Profile = session.guard(snapshot) {
        val principal = snapshot.principal ?: return@guard Profile(snapshot)
        dao.ensureAccount(AccountEntity(principal.accountId, "VK", principal.userId, principal.displayName))
        Profile(snapshot, dao.observeAthletes(principal.accountId).first().map(::toAthlete), dao.getAccount(principal.accountId)?.activeAthleteId)
    }

    suspend fun load(snapshot: SessionSnapshot, athleteId: Long): Athlete? = session.guard(snapshot) {
        dao.ownedAthlete(requireNotNull(snapshot.accountId), athleteId)?.let(::toAthlete)
    }

    suspend fun requireOwned(snapshot: SessionSnapshot, athleteId: Long) {
        requireNotNull(dao.ownedAthlete(requireNotNull(snapshot.accountId), athleteId)) { "Athlete is unavailable" }
    }

    suspend fun select(snapshot: SessionSnapshot, athleteId: Long) = session.guard(snapshot) {
        dao.selectAthlete(requireNotNull(snapshot.accountId), athleteId)
    }

    suspend fun save(snapshot: SessionSnapshot, id: Long?, name: String, birthDate: String, gender: Swimmer?, rank: SwimmingRank? = null): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 80) { "Invalid athlete name" }
        val date = AthleteBirthDate.parse(birthDate)
        return session.guard(snapshot) {
            dao.saveAthlete(AthleteEntity(id ?: 0, requireNotNull(snapshot.accountId), trimmed, date?.toString(), gender?.name, rank?.name))
        }
    }

    private fun toAthlete(row: AthleteEntity) = Athlete(row.id, row.accountId, row.name, row.birthDate?.let { LocalDate.parse(it) },
        row.sportsGender?.let { value -> Swimmer.entries.firstOrNull { it.name == value } },
        row.rank?.let { value -> SwimmingRank.entries.firstOrNull { it.name == value } })
}
