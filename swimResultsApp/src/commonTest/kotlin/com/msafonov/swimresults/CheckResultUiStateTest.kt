package com.msafonov.swimresults

import com.msafonov.swimresults.data.model.Constants
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.RankTableType
import com.msafonov.swimresults.data.model.SwimEvent
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.TimeComponents
import com.msafonov.swimresults.data.model.TimeComponents.Companion.toDuration
import com.msafonov.swimresults.data.storage.RankTableStorage
import com.msafonov.swimresults.viewmodel.CheckResultFilters
import com.msafonov.swimresults.viewmodel.CheckResultUiState
import com.msafonov.swimresults.viewmodel.StandardsStatus
import com.msafonov.swimresults.viewmodel.qualificationStandards
import com.msafonov.swimresults.viewmodel.withTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class CheckResultUiStateTest {
    @Test
    fun everyStyleAndPoolUsesDomainDistancesAndMinimumFallback() {
        for (style in SwimStyle.entries) {
            for (pool in listOf(PoolType.Short, PoolType.Long)) {
                val expected = when (style) {
                    SwimStyle.FREESTYLE -> Constants.FREESTYLE_LIST
                    SwimStyle.MEDLEY -> if (pool == PoolType.Short) {
                        Constants.MEDLEY_LIST_SHORT
                    } else Constants.MEDLEY_LIST_LONG
                    else -> Constants.COMMON_LIST
                }
                val invalid = CheckResultFilters(swimStyle = style, poolType = pool, distance = 999)
                assertEquals(expected, invalid.distances)
                assertEquals(expected.min(), invalid.normalized().distance)
                for (distance in expected) {
                    val valid = invalid.copy(distance = distance)
                    assertEquals(valid, valid.normalized())
                }
            }
        }
    }

    @Test
    fun transitionsPreserveValidDistanceAndUseConsistentFallback() {
        val medley = CheckResultFilters(swimStyle = SwimStyle.MEDLEY, distance = 100)
        assertEquals(200, medley.copy(poolType = PoolType.Long).normalized().distance)
        assertEquals(50, CheckResultFilters(distance = 800)
            .copy(swimStyle = SwimStyle.BREASTSTROKE).normalized().distance)
        assertEquals(200, medley.copy(distance = 200, swimStyle = SwimStyle.FREESTYLE)
            .normalized().distance)
    }

    @Test
    fun initialFiltersAndAllTableKeysMatchExistingDefaults() {
        val filters = CheckResultFilters()
        assertEquals(Swimmer.Women, filters.swimmer)
        assertEquals(PoolType.Short, filters.poolType)
        assertEquals(SwimStyle.FREESTYLE, filters.swimStyle)
        assertEquals(50, filters.distance)
        assertEquals(RankTableType.ShortWaterWomen, filters.tableType)
        assertEquals(RankTableType.ShortWaterMan, filters.copy(swimmer = Swimmer.Men).tableType)
        assertEquals(RankTableType.LongWaterWomen, filters.copy(poolType = PoolType.Long).tableType)
        assertEquals(RankTableType.LongWaterMan,
            filters.copy(poolType = PoolType.Long, swimmer = Swimmer.Men).tableType)
    }

    @Test
    fun projectionUsesRankRatherThanMapOrderOrTimeOrder() {
        val shuffledRanks = listOf(
            SwimmingRank.Second, SwimmingRank.InternationalMasterOfSports,
            SwimmingRank.FirstJunior, SwimmingRank.Third, SwimmingRank.MasterOfSports,
            SwimmingRank.ThirdJunior, SwimmingRank.CandidateMasterOfSports,
            SwimmingRank.SecondJunior, SwimmingRank.First
        )
        val map = shuffledRanks.mapIndexed { index, rank -> (index + 10).seconds to rank }.toMap()
        val standards = qualificationStandards(SwimEvent(50, SwimStyle.FREESTYLE, map))
        assertEquals(listOf(
            SwimmingRank.InternationalMasterOfSports, SwimmingRank.MasterOfSports,
            SwimmingRank.CandidateMasterOfSports, SwimmingRank.First, SwimmingRank.Second,
            SwimmingRank.Third, SwimmingRank.FirstJunior, SwimmingRank.SecondJunior,
            SwimmingRank.ThirdJunior
        ), standards.map { it.rank })
        standards.forEach { assertEquals(it.rank, map[it.time]) }
    }

    @Test
    fun partialAndAbsentDataNeverInventTimes() {
        val event = SwimEvent(50, SwimStyle.FREESTYLE, mapOf(30.seconds to SwimmingRank.First))
        val partial = qualificationStandards(event)
        assertEquals(9, partial.size)
        assertEquals(30.seconds, partial.single { it.rank == SwimmingRank.First }.time)
        assertEquals(8, partial.count { it.time == null })
        val ready = CheckResultUiState().withTable(listOf(event))
        assertEquals(StandardsStatus.Ready, ready.status)
        val missing = ready.copy(filters = ready.filters.copy(distance = 100)).withTable(listOf(event))
        assertEquals(StandardsStatus.Unavailable, missing.status)
        assertNull(missing.currentEvent)
        assertTrue(missing.standards.all { it.time == null })
        assertEquals(9, qualificationStandards(null).size)
    }

    @Test
    fun builtInTablesContainEverySupportedCombinationAndProjectOriginalValues() {
        val storage = RankTableStorage()
        for (swimmer in listOf(Swimmer.Men, Swimmer.Women)) {
            for (pool in listOf(PoolType.Short, PoolType.Long)) {
                for (style in SwimStyle.entries) {
                    val filters = CheckResultFilters(swimmer, pool, style).normalized()
                    val table = storage.rankTable(filters.tableType)
                    for (distance in filters.distances) {
                        val state = CheckResultUiState(filters = filters.copy(distance = distance)).withTable(table)
                        val event = assertNotNull(state.currentEvent)
                        assertEquals(StandardsStatus.Ready, state.status)
                        assertEquals(9, state.standards.size)
                        state.standards.forEach { standard ->
                            val time = assertNotNull(standard.time)
                            assertEquals(standard.rank, event.results[time])
                        }
                    }
                    if (style == SwimStyle.MEDLEY) {
                        assertEquals(pool == PoolType.Short,
                            table.any { it.style == style && it.distance == 100 })
                    }
                }
            }
        }
        // Values are from the existing local dataset, including its established precision.
        val men50 = storage.rankTable(RankTableType.ShortWaterMan)
            .single { it.style == SwimStyle.FREESTYLE && it.distance == 50 }
        val projected = qualificationStandards(men50)
        assertEquals(TimeComponents.from("21.18").toDuration(), projected.first().time)
        assertEquals(TimeComponents.from("55.05").toDuration(), projected.last().time)
    }
}
