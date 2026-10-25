package com.msafonov.swimresults.data.model

import com.msafonov.swimresults.auth.SessionSnapshot
import kotlinx.datetime.LocalDate

data class Athlete(
    val id: Long, val accountId: String, val name: String,
    val birthDate: LocalDate?, val sportsGender: Swimmer?, val rank: SwimmingRank? = null
)
data class Profile(val session: SessionSnapshot, val athletes: List<Athlete> = emptyList(), val activeAthleteId: Long? = null) {
    val activeAthlete: Athlete? get() = athletes.firstOrNull { it.id == activeAthleteId }
}

sealed interface HistoryLabel {
    data object DeviceLocal : HistoryLabel
    data class AthleteName(val name: String) : HistoryLabel
    data object SelectAthlete : HistoryLabel
    data object LoadError : HistoryLabel
}

/** Immutable target for a history operation or draft; active-athlete changes never retarget it. */
data class HistoryContext(val session: SessionSnapshot, val athleteId: Long? = null, val label: HistoryLabel = HistoryLabel.DeviceLocal, val available: Boolean = true) {
    init { require(athleteId == null || session.accountId != null) }
}
