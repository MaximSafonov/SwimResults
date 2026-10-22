package com.msafonov.swimresults

import com.msafonov.swimresults.data.local.dao.CompetitionWithSwims
import com.msafonov.swimresults.data.local.dao.SwimHistoryDao
import com.msafonov.swimresults.data.local.entity.CompetitionEntity
import com.msafonov.swimresults.data.local.entity.SwimRecordEntity
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.repository.CheckResultRepository
import com.msafonov.swimresults.data.repository.NewSwimRecord
import com.msafonov.swimresults.data.repository.SwimHistoryRepository
import com.msafonov.swimresults.data.storage.InMemoryStopWatchStorage
import com.msafonov.swimresults.data.storage.RankTableStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SwimHistoryRepositoryTest {
    @Test
    fun parsesResultTimeToIntegerCentiseconds() {
        assertEquals(8_345L, SwimHistoryRepository.parseTimeCentiseconds("1:23.45"))
        assertEquals(5_990L, SwimHistoryRepository.parseTimeCentiseconds("59.9"))
        assertEquals(30_000L, SwimHistoryRepository.parseTimeCentiseconds("5:00"))
    }

    @Test
    fun rejectsInvalidTimeValues() {
        assertNull(SwimHistoryRepository.parseTimeCentiseconds("1:60.00"))
        assertNull(SwimHistoryRepository.parseTimeCentiseconds("10.123"))
        assertNull(SwimHistoryRepository.parseTimeCentiseconds("abc"))
    }

    @Test
    fun saveUsesExistingQualificationRulesAndStoresSnapshot() = runTest {
        val dao = InMemoryHistoryDao()
        val repository = SwimHistoryRepository(
            dao,
            CheckResultRepository(RankTableStorage(), InMemoryStopWatchStorage())
        )
        repository.save(
            NewSwimRecord(
                competitionName = "Тестовый старт",
                swimDate = "2026-06-12",
                distanceMeters = 50,
                style = SwimStyle.FREESTYLE,
                gender = Swimmer.Women,
                pool = PoolType.Short,
                time = "10.00"
            )
        )
        assertTrue(dao.atomicInsertCalled)
        val saved = repository.history.first().single().swims.single()
        assertEquals(1_000L, saved.timeCentiseconds)
        assertEquals("InternationalMasterOfSports", saved.qualification?.name)
        assertEquals("2026-06-12", repository.history.first().single().competition.date)
    }
}

private class InMemoryHistoryDao : SwimHistoryDao() {
    private val competitions = mutableListOf<CompetitionEntity>()
    private val swims = mutableListOf<SwimRecordEntity>()
    private var nextCompetitionId = 1L
    private var nextSwimId = 1L
    var atomicInsertCalled = false
        private set

    override fun observeHistory(): Flow<List<CompetitionWithSwims>> = flow {
        emit(competitions.map { competition ->
            CompetitionWithSwims(competition, swims.filter { it.competitionId == competition.id })
        })
    }

    override fun observeCompetitions(): Flow<List<CompetitionEntity>> = flow { emit(competitions.toList()) }

    override suspend fun getCompetition(id: Long): CompetitionEntity? = competitions.firstOrNull { it.id == id }

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

    override suspend fun insertSwimWithCompetition(
        competitionName: String,
        competitionDate: String,
        swim: SwimRecordEntity
    ): Long {
        atomicInsertCalled = true
        return super.insertSwimWithCompetition(competitionName, competitionDate, swim)
    }
}
