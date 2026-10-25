package com.msafonov.swimresults

import com.msafonov.swimresults.data.local.dao.CompetitionWithSwims
import com.msafonov.swimresults.data.local.dao.SwimHistoryDao
import com.msafonov.swimresults.data.local.entity.CompetitionEntity
import com.msafonov.swimresults.data.local.entity.SwimRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class SwimHistoryDaoTest {
    @Test
    fun savesSwimWithCompetitionAndReusesMatchingCompetition() = runTest {
        val dao = InMemorySwimHistoryDao()
        val firstId = dao.insertSwimWithCompetition(
            "Кубок города",
            "2026-06-12",
            sampleSwim()
        )
        val repeatedId = dao.insertSwimWithCompetition(
            "Кубок города",
            "2026-06-12",
            sampleSwim()
        )

        val groups: List<CompetitionWithSwims> = dao.observeHistory().first()
        assertEquals(1, groups.size)
        assertEquals("Кубок города", groups.single().competition.name)
        assertEquals(2, groups.single().swims.size)
        assertNotEquals(firstId, repeatedId)
        assertEquals(setOf(firstId, repeatedId), groups.single().swims.map { it.id }.toSet())
        assertEquals(groups.single().competition.id, groups.single().swims.first().competitionId)
        assertEquals(groups.single().competition.id, groups.single().swims.last().competitionId)
        assertEquals(8_345L, groups.single().swims.first().timeCentiseconds)
    }

    @Test
    fun updateAndDeleteAffectOnlyTheSelectedIdAndMissingUpdateCreatesNothing() = runTest {
        val dao = InMemorySwimHistoryDao()
        val firstId = dao.insertSwimWithCompetition("Кубок", "2026-06-12", sampleSwim())
        val secondId = dao.insertSwimWithCompetition("Кубок", "2026-06-12", sampleSwim())
        val first = dao.getSwim(firstId)!!
        val second = dao.getSwim(secondId)
        assertEquals(1, dao.updateSwimWithCompetition(
            first.copy(timeCentiseconds = 9000), first.competitionId, "Кубок", "2026-07-01"
        ))
        assertEquals(second, dao.getSwim(secondId))
        assertEquals("2026-06-12", dao.getCompetition(first.competitionId)!!.date)
        assertEquals(1, dao.deleteSwim(firstId))
        assertNull(dao.getSwim(firstId))
        assertEquals(0, dao.deleteSwim(firstId))
        assertEquals(0, dao.updateSwimWithCompetition(first, null, "Не создавать", "2026-07-01"))
        assertEquals(1, dao.observeHistory().first().size)
        assertEquals(listOf(secondId), dao.observeHistory().first().single().swims.map { it.id })
    }
}

private fun sampleSwim() = SwimRecordEntity(
    competitionId = 0,
    swimDate = "2026-06-13",
    distanceMeters = 100,
    style = "FREESTYLE",
    gender = "Women",
    poolLengthMeters = 25,
    timeCentiseconds = 8_345,
    qualification = "First"
)

private class InMemorySwimHistoryDao : SwimHistoryDao() {
    override suspend fun ownedLinkTarget(accountId: String, id: Long): com.msafonov.swimresults.data.local.entity.AthleteEntity? = null
    override suspend fun selectedSwims(ids: List<Long>) = swims.filter { it.id in ids }
    override suspend fun assignUnassigned(ids: List<Long>, athleteId: Long): Int = error("No link target in this fixture")
    private val competitions = mutableListOf<CompetitionEntity>()
    private val swims = mutableListOf<SwimRecordEntity>()
    private var nextCompetitionId = 1L
    private var nextSwimId = 1L

    override fun observeHistory(): Flow<List<CompetitionWithSwims>> = flow {
        emit(competitions.map { competition ->
            CompetitionWithSwims(competition, swims.filter { it.competitionId == competition.id })
        })
    }

    override fun observeCompetitions(): Flow<List<CompetitionEntity>> = flow { emit(competitions.toList()) }

    override suspend fun getCompetition(id: Long): CompetitionEntity? = competitions.firstOrNull { it.id == id }

    override suspend fun getSwim(id: Long): SwimRecordEntity? = swims.firstOrNull { it.id == id }

    override suspend fun updateSwim(entity: SwimRecordEntity): Int {
        val index = swims.indexOfFirst { it.id == entity.id }
        if (index == -1) return 0
        swims[index] = entity
        return 1
    }

    override suspend fun deleteSwim(id: Long): Int = if (swims.removeAll { it.id == id }) 1 else 0

    override suspend fun findCompetition(name: String, date: String): CompetitionEntity? =
        competitions.firstOrNull { it.name == name && it.date == date }

    override suspend fun insertCompetition(entity: CompetitionEntity): Long {
        if (findCompetition(entity.name, entity.date) != null) return -1L
        val id = nextCompetitionId++
        competitions += entity.copy(id = id)
        return id
    }

    override suspend fun insertSwim(entity: SwimRecordEntity): Long {
        val id = nextSwimId++
        swims += entity.copy(id = id)
        return id
    }
}
