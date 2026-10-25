package com.msafonov.swimresults

import android.content.Context
import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.msafonov.swimresults.data.local.SwimHistoryDatabase
import com.msafonov.swimresults.data.local.entity.AccountEntity
import com.msafonov.swimresults.data.local.entity.AthleteEntity
import com.msafonov.swimresults.data.local.entity.CompetitionEntity
import com.msafonov.swimresults.data.local.entity.SwimRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlin.test.*

/** All files are disposable, unique test-cache databases. Never opens the application database. */
@RunWith(AndroidJUnit4::class)
class SwimHistoryDatabaseTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().context
    private val driver get() = BundledSQLiteDriver()
    private fun file() = File(context.cacheDir, "profile-test-${UUID.randomUUID()}.db")
    private fun open(file: File) = Room.databaseBuilder<SwimHistoryDatabase>(context, file.absolutePath)
        .setDriver(driver).setQueryCoroutineContext(Dispatchers.IO).build()

    private val records = listOf(
        SwimRecordEntity(10, 3, "2026-06-13", 100, "FREESTYLE", "Women", 25, 8345, "First"),
        SwimRecordEntity(20, 3, "2026-06-14", 50, "BACKSTROKE", "Men", 50, 3000, null),
        SwimRecordEntity(30, 7, "2026-06-15", 200, "MEDLEY", "Women", 25, 15000, "Second")
    )
    private val competitions = listOf(CompetitionEntity(3, "Первый старт", "2026-06-12"), CompetitionEntity(7, "Второй старт", "2026-06-15"))

    @Test fun freshV3StoresAthleteMetadataAndPreservesOwnershipAndTransactions() = runBlocking {
        val file = file()
        try {
            val database = open(file)
            try {
                val dao = database.swimHistoryDao()
                competitions.forEach { dao.insertCompetition(it) }
                records.forEach { dao.insertSwim(it) }
                assertEquals(records, records.map { dao.getSwim(it.id) })
                driver.open(file.absolutePath).use { connection ->
                    assertEquals(3L, scalar(connection, "PRAGMA user_version"))
                    assertEquals(0L, scalar(connection, "SELECT count(*) FROM pragma_foreign_key_check"))
                    assertEquals(1L, scalar(connection, "SELECT count(*) FROM pragma_foreign_key_list('swim_records') WHERE [table]='athletes' AND [from]='athleteId' AND on_delete='RESTRICT'"))
                    assertEquals(1L, scalar(connection, "SELECT count(*) FROM pragma_index_list('swim_records') WHERE name='index_swim_records_athleteId'"))
                }
                exerciseLinkTransactions(database)
            } finally { database.close() }
        } finally { cleanup(file) }
    }

    private suspend fun exerciseLinkTransactions(database: SwimHistoryDatabase) {
        val dao = database.swimHistoryDao()
        val profiles = database.profileDao()
        profiles.ensureAccount(AccountEntity("vk:A", "VK", "A", "A"))
        profiles.ensureAccount(AccountEntity("vk:B", "VK", "B", "B"))
        val one = profiles.saveAthlete(AthleteEntity(accountId = "vk:A", name = "Первый", birthDate = "2010-02-03", rank = "Second", sportsGender = null))
        val two = profiles.saveAthlete(AthleteEntity(accountId = "vk:A", name = "Второй", birthDate = null, sportsGender = "Men"))
        val foreign = profiles.saveAthlete(AthleteEntity(accountId = "vk:B", name = "Чужой", birthDate = null, sportsGender = null))
        assertEquals("2010-02-03", profiles.ownedAthlete("vk:A", one)!!.birthDate)
        assertEquals("Second", profiles.ownedAthlete("vk:A", one)!!.rank)
        profiles.saveAthlete(profiles.ownedAthlete("vk:A", one)!!.copy(birthDate = "2012-02-29", rank = "FirstJunior", sportsGender = "Women"))
        assertEquals("2012-02-29", profiles.ownedAthlete("vk:A", one)!!.birthDate)
        assertEquals("FirstJunior", profiles.ownedAthlete("vk:A", one)!!.rank)
        assertEquals("Women", profiles.ownedAthlete("vk:A", one)!!.sportsGender)
        assertNull(profiles.ownedAthlete("vk:A", two)!!.birthDate)
        assertNull(profiles.ownedAthlete("vk:A", two)!!.rank)
        assertNull(profiles.ownedAthlete("vk:B", one))
        assertFailsWith<IllegalArgumentException> { dao.linkDeviceRecords("vk:A", foreign, listOf(10)) }
        assertFailsWith<IllegalArgumentException> { dao.linkDeviceRecords("vk:A", 99999, listOf(10)) }
        assertFailsWith<IllegalArgumentException> { dao.linkDeviceRecords("vk:A", one, listOf(10, 10)) }
        assertFailsWith<IllegalArgumentException> { dao.linkDeviceRecords("vk:A", one, (1L..201L).toList()) }
        assertFailsWith<IllegalStateException> { dao.linkDeviceRecords("vk:A", one, listOf(10, 99999)) }
        assertEquals(records[0], dao.getSwim(10))
        dao.linkDeviceRecords("vk:A", one, listOf(20))
        assertEquals(records[1].copy(athleteId = one), dao.getSwim(20))
        assertFailsWith<IllegalStateException> { dao.linkDeviceRecords("vk:A", two, listOf(10, 20)) }
        assertEquals(records[0], dao.getSwim(10)) // Missing/mixed requests never partially assign 10.
        assertEquals(records[1].copy(athleteId = one), dao.getSwim(20))
        assertFailsWith<IllegalStateException> { dao.linkDeviceRecords("vk:A", two, listOf(20)) }
        val wins = coroutineScope {
            listOf(one, two).map { target -> async(Dispatchers.IO) { runCatching { dao.linkDeviceRecords("vk:A", target, listOf(30)) }.isSuccess } }.awaitAll()
        }
        assertEquals(1, wins.count { it })
        val winner = dao.getSwim(30)!!
        assertTrue(winner.athleteId in listOf(one, two))
        assertEquals(records[2].copy(athleteId = winner.athleteId), winner)
        assertEquals(competitions.toSet(), dao.observeHistory().first().map { it.competition }.toSet())
        assertFails { profiles.insertAthlete(AthleteEntity(accountId = "missing", name = "Invalid FK", birthDate = null, sportsGender = null)) }
        assertFails { dao.insertSwim(records[0].copy(id = 999, athleteId = 99999)) }
        assertFailsWith<IllegalArgumentException> { profiles.selectAthlete("vk:A", foreign) }
        assertEquals(one, profiles.getAccount("vk:A")!!.activeAthleteId)
    }

    private fun scalar(connection: SQLiteConnection, sql: String): Long = connection.prepare(sql).use { statement ->
        check(statement.step()); statement.getLong(0)
    }
    private fun cleanup(file: File) {
        listOf(file, File(file.path + "-wal"), File(file.path + "-shm")).forEach { it.delete() }
    }
}
