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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import com.msafonov.swimresults.data.storage.RankTable
import com.msafonov.swimresults.data.model.RankTableType
import com.msafonov.swimresults.data.model.SwimEvent
import kotlin.test.assertFailsWith
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
        val repository = historyTestRepository(dao)
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

    @Test
    fun metadataEditKeepsCompetitionIdentitySiblingAndHistoricalQualification() = runTest {
        val dao = InMemoryHistoryDao()
        val id = seedHistory(dao)
        val siblingId = seedHistory(dao)
        val sibling = dao.getSwim(siblingId)
        val original = dao.getSwim(id)!!
        val repository = historyTestRepository(dao)
        repository.update(id, historyTestInput().copy(swimDate = "2026-06-14"), original.competitionId)
        val updated = dao.getSwim(id)!!
        assertEquals(original.copy(swimDate = "2026-06-14"), updated)
        assertEquals(sibling, dao.getSwim(siblingId))
        assertEquals("2026-06-12", dao.competitions.single().date)
        assertEquals(2, dao.swims.size)
        assertEquals(id, repository.load(id)!!.first.id)
    }

    @Test
    fun unchangedSaveDoesNotWriteAndAthleticEditRecalculatesQualification() = runTest {
        val dao = InMemoryHistoryDao()
        val id = seedHistory(dao)
        val competitionId = dao.getSwim(id)!!.competitionId
        val repository = historyTestRepository(dao)
        repository.update(id, historyTestInput(), competitionId)
        assertEquals(0, dao.updateCalls)
        repository.update(id, historyTestInput().copy(time = "10.00"), competitionId)
        assertEquals("InternationalMasterOfSports", dao.getSwim(id)!!.qualification)
        assertEquals(1, dao.updateCalls)
    }

    @Test
    fun explicitCompetitionSelectionUsesItsIdAndNewChoiceCreatesSeparateGroup() = runTest {
        val dao = InMemoryHistoryDao()
        val id = seedHistory(dao)
        val other = dao.insertCompetition(CompetitionEntity(name = "Кубок города", date = "2026-05-01"))
        val repository = historyTestRepository(dao)
        val visibleSibling = dao.insertSwim(dao.getSwim(id)!!.copy(id = 0, competitionId = other))
        repository.update(id, historyTestInput(), other)
        assertEquals(other, dao.getSwim(id)!!.competitionId)
        assertEquals("2026-06-13", dao.getSwim(id)!!.swimDate)
        repository.update(id, historyTestInput().copy(competitionName = "Новый старт"), null)
        assertEquals("Новый старт", repository.load(id)!!.second.name)
        assertEquals("2026-06-13", repository.load(id)!!.second.date)
        assertEquals(3, dao.competitions.size)
        assertEquals(listOf(id, visibleSibling), dao.swims.map { it.id })
    }

    @Test
    fun missingEditAndDeleteNeverInsertAndInvalidInputDoesNotWrite() = runTest {
        val dao = InMemoryHistoryDao()
        val repository = historyTestRepository(dao)
        assertNull(repository.load(99))
        assertFailsWith<NoSuchElementException> { repository.update(99, historyTestInput(), null) }
        assertFailsWith<NoSuchElementException> { repository.delete(99) }
        assertTrue(dao.competitions.isEmpty())
        assertTrue(dao.swims.isEmpty())
        val id = seedHistory(dao)
        assertFailsWith<IllegalArgumentException> {
            repository.update(id, historyTestInput().copy(time = "1:60.00"), 1)
        }
        assertEquals(0, dao.updateCalls)
    }

    @Test
    fun qualificationCancellationPropagatesWithoutInserting() = runTest {
        val dao = InMemoryHistoryDao()
        val tables = object : RankTable {
            override fun rankTable(type: RankTableType): List<SwimEvent> = throw CancellationException("cancel")
        }
        val repository = historyTestRepository(dao, tables = tables)
        assertFailsWith<CancellationException> { repository.save(historyTestInput()) }
        assertTrue(dao.swims.isEmpty())
        assertTrue(dao.competitions.isEmpty())
    }
}

// Reactive fake shared by the focused repository and ViewModel tests; no SQLite claims.
internal class InMemoryHistoryDao : SwimHistoryDao() {
    val profileDao = InMemoryProfileDao()
    override suspend fun ownedLinkTarget(accountId: String, id: Long) = profileDao.ownedAthlete(accountId, id)
    override suspend fun selectedSwims(ids: List<Long>) = swims.filter { it.id in ids }
    override suspend fun assignUnassigned(ids: List<Long>, athleteId: Long): Int {
        beforeWrite()
        val rows = swims.filter { it.id in ids && it.athleteId == null }
        rows.forEach { row -> swims[swims.indexOfFirst { it.id == row.id }] = row.copy(athleteId = athleteId) }
        emitChanges()
        return rows.size
    }
    val competitions = mutableListOf<CompetitionEntity>()
    val swims = mutableListOf<SwimRecordEntity>()
    private val version = MutableStateFlow(0)
    private var nextCompetitionId = 1L
    private var nextSwimId = 1L
    var atomicInsertCalled = false
        private set
    var updateCalls = 0
        private set
    var deleteCalls = 0
        private set
    var getFailure: Exception? = null
    var beforeWrite: suspend () -> Unit = {}

    fun emitChanges() { version.value++ }

    override fun observeHistory(): Flow<List<CompetitionWithSwims>> = version.map {
        competitions.map { competition ->
            CompetitionWithSwims(competition, swims.filter { it.competitionId == competition.id })
        }
    }

    override fun observeCompetitions(): Flow<List<CompetitionEntity>> = version.map { competitions.toList() }

    override suspend fun getCompetition(id: Long): CompetitionEntity? = competitions.firstOrNull { it.id == id }

    override suspend fun getSwim(id: Long): SwimRecordEntity? {
        getFailure?.let { throw it }
        return swims.firstOrNull { it.id == id }
    }

    override suspend fun updateSwim(entity: SwimRecordEntity): Int {
        updateCalls++
        beforeWrite()
        val index = swims.indexOfFirst { it.id == entity.id }
        if (index == -1) return 0
        swims[index] = entity
        emitChanges()
        return 1
    }

    override suspend fun deleteSwim(id: Long): Int {
        deleteCalls++
        beforeWrite()
        val removed = swims.removeAll { it.id == id }
        emitChanges()
        return if (removed) 1 else 0
    }

    override suspend fun findCompetition(name: String, date: String): CompetitionEntity? =
        competitions.firstOrNull { it.name == name && it.date == date }

    override suspend fun insertCompetition(entity: CompetitionEntity): Long {
        if (findCompetition(entity.name, entity.date) != null) return -1L
        val id = nextCompetitionId++
        competitions += entity.copy(id = id)
        emitChanges()
        return id
    }

    override suspend fun insertSwim(entity: SwimRecordEntity): Long {
        beforeWrite()
        val id = nextSwimId++
        swims += entity.copy(id = id)
        emitChanges()
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

internal fun historyTestRepository(
    dao: SwimHistoryDao,
    session: com.msafonov.swimresults.auth.AccountSession = com.msafonov.swimresults.auth.AccountSession(),
    profileDao: InMemoryProfileDao = (dao as? InMemoryHistoryDao)?.profileDao ?: InMemoryProfileDao(),
    tables: RankTable = RankTableStorage()
): SwimHistoryRepository = SwimHistoryRepository(
    dao, CheckResultRepository(tables, InMemoryStopWatchStorage()), session,
    com.msafonov.swimresults.data.repository.ProfileRepository(profileDao, session)
)

internal fun historyTestInput() = NewSwimRecord(
    competitionName = "Кубок города", swimDate = "2026-06-13", distanceMeters = 100,
    style = SwimStyle.FREESTYLE, gender = Swimmer.Women, pool = PoolType.Short, time = "1:23.45"
)

internal suspend fun seedHistory(dao: InMemoryHistoryDao): Long = dao.insertSwimWithCompetition(
    "Кубок города", "2026-06-12", SwimRecordEntity(
        competitionId = 0, swimDate = "2026-06-13", distanceMeters = 100,
        style = "FREESTYLE", gender = "Women", poolLengthMeters = 25,
        timeCentiseconds = 8_345, qualification = "First"
    )
)
