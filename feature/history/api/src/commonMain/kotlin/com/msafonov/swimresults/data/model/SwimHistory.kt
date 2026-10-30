package com.msafonov.swimresults.data.model

data class SwimCompetition(
    val id: Long,
    val name: String,
    val date: String
)

data class RecordedSwim(
    val id: Long,
    val competitionId: Long,
    val swimDate: String,
    val distanceMeters: Int,
    val style: SwimStyle,
    val gender: Swimmer,
    val poolLengthMeters: Int,
    val timeCentiseconds: Long,
    val qualification: SwimmingRank?
)

data class SwimCompetitionGroup(
    val competition: SwimCompetition,
    val swims: List<RecordedSwim>
)
