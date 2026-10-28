package com.msafonov.swimresults

import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.model.WorldAquaticsPoints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorldAquaticsPointsTest {
    @Test
    fun baseTimesScoreExactlyOneThousandForBothCoursesAndSexes() {
        for (pool in PoolType.entries) for (swimmer in Swimmer.entries) {
            val base = assertNotNull(WorldAquaticsPoints.baseCentiseconds(pool, swimmer, SwimStyle.FREESTYLE, 100))
            assertEquals(1000, WorldAquaticsPoints.score(pool, swimmer, SwimStyle.FREESTYLE, 100, base))
        }
    }

    @Test
    fun shortCourseFiftyFreestyleBasesAreReachableForMenAndWomen() {
        assertEquals(1990L, WorldAquaticsPoints.baseCentiseconds(PoolType.Short, Swimmer.Men, SwimStyle.FREESTYLE, 50))
        assertEquals(2283L, WorldAquaticsPoints.baseCentiseconds(PoolType.Short, Swimmer.Women, SwimStyle.FREESTYLE, 50))
    }

    @Test
    fun fasterAndSlowerTimesUseFormulaAndTruncate() {
        assertTrue(WorldAquaticsPoints.score(PoolType.Long, Swimmer.Men, SwimStyle.FREESTYLE, 100, 4600)!! > 1000)
        assertEquals(799, WorldAquaticsPoints.score(PoolType.Long, Swimmer.Men, SwimStyle.FREESTYLE, 100, 5000))
    }

    @Test
    fun allAppSupportedEventCombinationsHaveBases() {
        val distances = mapOf(
            SwimStyle.FREESTYLE to listOf(50, 100, 200, 400, 800, 1500),
            SwimStyle.BACKSTROKE to listOf(50, 100, 200),
            SwimStyle.BREASTSTROKE to listOf(50, 100, 200),
            SwimStyle.BUTTERFLY to listOf(50, 100, 200),
            SwimStyle.MEDLEY to listOf(100, 200, 400)
        )
        for (pool in PoolType.entries) for (swimmer in Swimmer.entries) {
            for ((style, events) in distances) for (distance in events) {
                if (pool == PoolType.Long && style == SwimStyle.MEDLEY && distance == 100) continue
                assertNotNull(WorldAquaticsPoints.baseCentiseconds(pool, swimmer, style, distance))
            }
        }
        assertNull(WorldAquaticsPoints.baseCentiseconds(PoolType.Long, Swimmer.Men, SwimStyle.MEDLEY, 100))
    }

    @Test
    fun invalidAndUnmappedTimesHaveNoScore() {
        assertNull(WorldAquaticsPoints.score(PoolType.Short, Swimmer.Women, SwimStyle.FREESTYLE, 100, 0))
        assertNull(WorldAquaticsPoints.score(PoolType.Short, Swimmer.Women, SwimStyle.FREESTYLE, 100, -1))
        assertNull(WorldAquaticsPoints.score(PoolType.Long, Swimmer.Men, SwimStyle.MEDLEY, 100, 10_000))
    }
}
