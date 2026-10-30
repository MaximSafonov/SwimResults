package com.msafonov.swimresults

import com.msafonov.swimresults.data.model.CheckedResult
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.SwimEvent
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.viewmodel.CheckResultFilters
import com.msafonov.swimresults.viewmodel.ResultComparisonStatus
import com.msafonov.swimresults.viewmodel.buildResultDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ResultDetailsUiStateTest {
    private val filters = CheckResultFilters(Swimmer.Women, PoolType.Short, SwimStyle.FREESTYLE, 100)
    private val all = linkedMapOf(
        60.seconds to SwimmingRank.InternationalMasterOfSports,
        65.seconds to SwimmingRank.MasterOfSports,
        70.seconds to SwimmingRank.CandidateMasterOfSports,
        75.seconds to SwimmingRank.First,
        80.seconds to SwimmingRank.Second,
        85.seconds to SwimmingRank.Third,
        90.seconds to SwimmingRank.FirstJunior,
        95.seconds to SwimmingRank.SecondJunior,
        100.seconds to SwimmingRank.ThirdJunior
    )

    @Test
    fun achievedMiddleRankShowsTwoAvailableRanksOnEachSideInExplicitOrder() {
        val details = details(all, SwimmingRank.Second, "1:20.00")
        assertEquals(listOf(SwimmingRank.CandidateMasterOfSports, SwimmingRank.First, SwimmingRank.Second,
            SwimmingRank.Third, SwimmingRank.FirstJunior), details.rows.map { it.rank })
        assertEquals(ResultComparisonStatus.Available, details.comparisonStatus)
    }

    @Test
    fun missingIntermediateStandardSearchesFurtherWithoutFillingOppositeSide() {
        val sparse = all.filterValues { it !in setOf(SwimmingRank.First, SwimmingRank.Third) }
        val details = details(sparse, SwimmingRank.Second, "1:20.00")
        assertEquals(listOf(SwimmingRank.MasterOfSports, SwimmingRank.CandidateMasterOfSports,
            SwimmingRank.Second, SwimmingRank.FirstJunior, SwimmingRank.SecondJunior), details.rows.map { it.rank })
    }

    @Test
    fun highestAndLowestQualificationsShowOnlyExistingNeighbors() {
        assertEquals(3, details(all, SwimmingRank.InternationalMasterOfSports, "1:00.00").rows.size)
        assertEquals(3, details(all, SwimmingRank.ThirdJunior, "1:40.00").rows.size)
    }

    @Test
    fun missingAchievedStandardIsExplicitAndDoesNotInventHighlightedRow() {
        val details = details(all.filterValues { it != SwimmingRank.Second }, SwimmingRank.Second, "1:20.00")
        assertEquals(ResultComparisonStatus.MissingAchievedStandard, details.comparisonStatus)
        assertTrue(details.rows.isEmpty())
    }

    @Test
    fun noQualificationSelectsClosestThenDisplaysInHierarchyOrder() {
        val details = details(all, null, "1:17.50")
        assertEquals(ResultComparisonStatus.NoQualification, details.comparisonStatus)
        assertEquals(5, details.rows.size)
        assertEquals(listOf(SwimmingRank.MasterOfSports, SwimmingRank.CandidateMasterOfSports,
            SwimmingRank.First, SwimmingRank.Second, SwimmingRank.Third), details.rows.map { it.rank })
    }

    @Test
    fun noResultAndMalformedTablesHaveSeparateUnavailableStates() {
        assertEquals(ResultComparisonStatus.NoResult, details(all, null, "1:20.00", checked = null).comparisonStatus)
        val duplicateRank = mapOf(80.seconds to SwimmingRank.Second, 81.seconds to SwimmingRank.Second)
        assertEquals(ResultComparisonStatus.StandardsUnavailable, details(duplicateRank, null, "1:20.00").comparisonStatus)
    }

    private fun details(
        standards: Map<kotlin.time.Duration, SwimmingRank>,
        rank: SwimmingRank?,
        input: String,
        checked: CheckedResult? = CheckedResult(rank, null, null, null)
    ) = buildResultDetails(filters, input, SwimEvent(100, SwimStyle.FREESTYLE, standards), checked)
}
