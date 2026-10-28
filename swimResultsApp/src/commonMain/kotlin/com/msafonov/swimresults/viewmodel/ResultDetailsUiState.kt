package com.msafonov.swimresults.viewmodel

import com.msafonov.swimresults.data.model.CheckedResult
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.SwimEvent
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.WorldAquaticsPoints
import kotlin.time.Duration

enum class ResultComparisonStatus { Available, NoQualification, NoResult, StandardsUnavailable, UnsupportedTime, MissingAchievedStandard }

data class ResultStandardRow(
    val rank: SwimmingRank,
    val time: Duration,
    val differenceCentiseconds: Long
)

data class ResultDetailsUiState(
    val distance: Int,
    val swimStyle: SwimStyle,
    val swimmer: Swimmer,
    val poolType: PoolType,
    val input: String,
    val checkedResult: CheckedResult?,
    val comparisonStatus: ResultComparisonStatus,
    val rows: List<ResultStandardRow>,
    val worldAquaticsPoints: Int? = null
)

internal val swimmingRankHierarchy = listOf(
    SwimmingRank.InternationalMasterOfSports,
    SwimmingRank.MasterOfSports,
    SwimmingRank.CandidateMasterOfSports,
    SwimmingRank.First,
    SwimmingRank.Second,
    SwimmingRank.Third,
    SwimmingRank.FirstJunior,
    SwimmingRank.SecondJunior,
    SwimmingRank.ThirdJunior
)

internal fun buildResultDetails(
    filters: CheckResultFilters,
    input: String,
    event: SwimEvent?,
    checked: CheckedResult?
): ResultDetailsUiState {
    fun state(status: ResultComparisonStatus, rows: List<ResultStandardRow> = emptyList()) =
        ResultDetailsUiState(filters.distance, filters.swimStyle, filters.swimmer, filters.poolType, input, checked, status, rows,
            if (checked == null) null else ResultDetailsTime.inputCentiseconds(input)?.let {
                WorldAquaticsPoints.score(filters.poolType, filters.swimmer, filters.swimStyle, filters.distance, it)
            })
    if (checked == null) return state(ResultComparisonStatus.NoResult)
    val resultTime = ResultDetailsTime.inputCentiseconds(input)
        ?: return state(ResultComparisonStatus.UnsupportedTime)
    val raw = event?.results.orEmpty()
    if (raw.isEmpty()) return state(ResultComparisonStatus.StandardsUnavailable)
    val byRank = mutableMapOf<SwimmingRank, Duration>()
    for ((time, rank) in raw) {
        if (rank in byRank) return state(ResultComparisonStatus.StandardsUnavailable)
        val cs = ResultDetailsTime.standardCentiseconds(time)
            ?: return state(ResultComparisonStatus.StandardsUnavailable)
        byRank[rank] = time
        if (ResultDetailsTime.difference(resultTime, cs) == null) {
            return state(ResultComparisonStatus.StandardsUnavailable)
        }
    }
    if (checked.currentRank == null) {
        val closest = byRank.mapNotNull { (rank, time) ->
            val standard = ResultDetailsTime.standardCentiseconds(time) ?: return@mapNotNull null
            val difference = ResultDetailsTime.difference(resultTime, standard) ?: return@mapNotNull null
            Triple(rank, time, difference)
        }.sortedWith(compareBy<Triple<SwimmingRank, Duration, Long>> { kotlin.math.abs(it.third) }
            .thenBy { swimmingRankHierarchy.indexOf(it.first) })
            .take(5).sortedBy { swimmingRankHierarchy.indexOf(it.first) }
            .map { ResultStandardRow(it.first, it.second, it.third) }
        return if (closest.isEmpty()) state(ResultComparisonStatus.StandardsUnavailable)
        else state(ResultComparisonStatus.NoQualification, closest)
    }
    if (checked.currentRank !in byRank) return state(ResultComparisonStatus.MissingAchievedStandard)
    val achievedIndex = swimmingRankHierarchy.indexOf(checked.currentRank)
    val stronger = swimmingRankHierarchy.take(achievedIndex).asReversed()
        .mapNotNull { rank -> byRank[rank]?.let { rank to it } }.take(2)
    val weaker = swimmingRankHierarchy.drop(achievedIndex + 1)
        .mapNotNull { rank -> byRank[rank]?.let { rank to it } }.take(2)
    val selected = (stronger + listOf(checked.currentRank to byRank.getValue(checked.currentRank)) + weaker)
        .toMap()
    val rows = swimmingRankHierarchy.filter { it in selected }.map { rank ->
        val time = selected.getValue(rank)
        val standard = ResultDetailsTime.standardCentiseconds(time)
            ?: return state(ResultComparisonStatus.StandardsUnavailable)
        ResultStandardRow(rank, time, ResultDetailsTime.difference(resultTime, standard)
            ?: return state(ResultComparisonStatus.StandardsUnavailable))
    }
    return state(ResultComparisonStatus.Available, rows)
}
