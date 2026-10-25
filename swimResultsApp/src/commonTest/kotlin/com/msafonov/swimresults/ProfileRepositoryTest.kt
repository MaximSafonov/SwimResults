package com.msafonov.swimresults

import com.msafonov.swimresults.auth.*
import com.msafonov.swimresults.data.model.HistoryLabel
import com.msafonov.swimresults.data.model.HistoryContext
import com.msafonov.swimresults.data.model.SwimmingRank
import kotlinx.datetime.LocalDate
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ProfileRepositoryTest {
    @Test fun validationOptionalFieldsAndActiveChoiceSurviveRepositoryRecreation() = runTest {
        val dao = InMemoryProfileDao()
        val profiles = profileFixture(dao)
        val snapshot = profiles.session.current
        for (name in listOf(" ", "x".repeat(81))) assertFailsWith<IllegalArgumentException> { profiles.save(snapshot, null, name, "", null) }
        for (date in listOf("31.12.1899", "01.01.9999", "29.02.2023", "31.04.2010", "1.01.2000", "2010", "abcd")) assertFailsWith<IllegalArgumentException> { profiles.save(snapshot, null, "Имя", date, null) }
        val first = profiles.save(snapshot, null, "  Первый  ", "", null)
        val second = profiles.save(snapshot, null, "Второй", "29.02.2012", Swimmer.Men, SwimmingRank.Second)
        assertEquals(first, profiles.current().activeAthleteId)
        assertEquals("Первый", profiles.load(snapshot, first)!!.name)
        assertNull(profiles.load(snapshot, first)!!.sportsGender)
        assertNull(profiles.load(snapshot, first)!!.birthDate)
        assertNull(profiles.load(snapshot, first)!!.rank)
        assertEquals("2012-02-29", dao.athletes[second]!!.birthDate)
        assertEquals("Second", dao.athletes[second]!!.rank)
        assertEquals(LocalDate(2012, 2, 29), profiles.load(snapshot, second)!!.birthDate)
        assertEquals(SwimmingRank.Second, profiles.load(snapshot, second)!!.rank)
        profiles.select(snapshot, second)
        val restarted = ProfileRepository(dao, profiles.session)
        assertEquals(second, restarted.current().activeAthleteId)
        profiles.save(snapshot, first, "Изменён", "03.04.2011", Swimmer.Women, SwimmingRank.FirstJunior)
        assertEquals(LocalDate(2011, 4, 3), profiles.load(snapshot, first)!!.birthDate)
        assertEquals(SwimmingRank.FirstJunior, profiles.load(snapshot, first)!!.rank)
        profiles.save(snapshot, first, "Изменён", "", null)
        assertNull(profiles.load(snapshot, first)!!.birthDate)
        assertNull(profiles.load(snapshot, first)!!.rank)
        assertEquals(2, profiles.current().athletes.size)
        assertEquals(second, profiles.current().activeAthleteId)
    }

    @Test fun foreignAthletesAndStaleSessionsCannotReadEditOrSelect() = runTest {
        val dao = InMemoryProfileDao()
        val profiles = profileFixture(dao)
        val old = profiles.session.current
        val id = profiles.save(old, null, "A", "", null)
        signedIn(profiles.session, "B")
        val current = profiles.session.current
        assertTrue(profiles.current().athletes.isEmpty())
        assertNull(profiles.load(current, id))
        assertFailsWith<IllegalArgumentException> { profiles.select(current, id) }
        assertFailsWith<IllegalStateException> { profiles.save(current, id, "B", "", null) }
        assertFailsWith<StaleSessionException> { profiles.load(old, id) }
        profiles.session.change(AuthState.Guest())
        assertFailsWith<IllegalArgumentException> { profiles.save(profiles.session.current, null, "Guest", "", null) }
        assertEquals("A", dao.athletes[id]!!.name)
    }

    @Test fun historyReadHintsCrudAndExplicitLinkRespectAccountAthleteAndDeviceBuckets() = runTest {
        val dao = InMemoryHistoryDao()
        val local = seedHistory(dao)
        val session = AccountSession()
        signedIn(session)
        val repository = historyTestRepository(dao, session)
        val profiles = repository.profiles
        profiles.current()
        assertEquals(HistoryLabel.SelectAthlete, repository.currentContext().label)
        assertFalse(repository.currentContext().available)
        val first = profiles.save(session.current, null, "Первый", "", null)
        val second = profiles.save(session.current, null, "Второй", "", null)
        assertEquals(HistoryLabel.AthleteName("Первый"), repository.currentContext().label)
        val a = HistoryContext(session.current, first, HistoryLabel.AthleteName("Первый"))
        val b = HistoryContext(session.current, second, HistoryLabel.AthleteName("Второй"))
        repository.save(historyTestInput().copy(competitionName = "Только первый"), a)
        val owned = dao.swims.last()
        assertNull(repository.load(owned.id, b))
        assertTrue(repository.competitions(b).first().isEmpty())
        assertFailsWith<NoSuchElementException> { repository.delete(owned.id, b) }
        assertFailsWith<NoSuchElementException> { repository.update(owned.id, historyTestInput(), null, b) }
        repository.save(historyTestInput().copy(competitionName = "Второй"), b)
        assertFailsWith<IllegalStateException> { repository.update(owned.id, historyTestInput(), dao.swims.last().competitionId, a) }
        repository.update(owned.id, historyTestInput().copy(competitionName = "Только первый"), owned.competitionId, a)
        assertEquals(first, dao.getSwim(owned.id)!!.athleteId)
        val before = dao.getSwim(local)!!
        repository.linkDeviceRecords(session.current, second, listOf(local))
        assertEquals(before.copy(athleteId = second), dao.getSwim(local))
        assertFailsWith<IllegalStateException> { repository.linkDeviceRecords(session.current, first, listOf(local)) }
        session.change(AuthState.Guest())
        assertEquals(HistoryLabel.DeviceLocal, repository.currentContext().label)
        assertTrue(repository.history(HistoryContext(session.current)).first().isEmpty())
        assertFailsWith<IllegalArgumentException> { repository.linkDeviceRecords(session.current, first, listOf(local)) }
        assertFailsWith<StaleSessionException> { repository.delete(owned.id, a) }
        signedIn(session, "B")
        profiles.current()
        val forged = HistoryContext(session.current, first, HistoryLabel.AthleteName("Чужой ID"))
        assertFailsWith<IllegalArgumentException> { repository.load(owned.id, forged) }
        assertFailsWith<IllegalArgumentException> { repository.delete(owned.id, forged) }
        assertFailsWith<IllegalArgumentException> { repository.competitions(forged).first() }
    }

    @Test fun linkingRejectsDuplicateOversizedMissingMixedForeignAndCancelledRequestsWithoutMutation() = runTest {
        val dao = InMemoryHistoryDao()
        val one = seedHistory(dao)
        val two = seedHistory(dao)
        val session = AccountSession()
        signedIn(session)
        val repository = historyTestRepository(dao, session)
        repository.profiles.current()
        val target = repository.profiles.save(session.current, null, "A", "", null)
        val before = dao.swims.toList()
        for (ids in listOf(emptyList(), listOf(one, one), (1L..201L).toList()))
            assertFailsWith<IllegalArgumentException> { repository.linkDeviceRecords(session.current, target, ids) }
        assertFailsWith<IllegalStateException> { repository.linkDeviceRecords(session.current, target, listOf(one, 999)) }
        assertFailsWith<IllegalArgumentException> { repository.linkDeviceRecords(session.current, 999, listOf(one)) }
        assertEquals(before, dao.swims)
        dao.beforeWrite = { throw CancellationException("cancel") }
        assertFailsWith<CancellationException> { repository.linkDeviceRecords(session.current, target, listOf(one, two)) }
        assertEquals(before, dao.swims)
        dao.beforeWrite = {}
        repository.linkDeviceRecords(session.current, target, listOf(two))
        assertFailsWith<IllegalStateException> { repository.linkDeviceRecords(session.current, target, listOf(one, two)) }
        assertEquals(before.first(), dao.getSwim(one))
        signedIn(session, "B")
        assertFailsWith<IllegalArgumentException> { repository.linkDeviceRecords(session.current, target, listOf(one)) }
    }
}
