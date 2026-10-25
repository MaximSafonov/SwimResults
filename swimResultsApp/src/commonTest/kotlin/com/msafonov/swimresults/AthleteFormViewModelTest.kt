package com.msafonov.swimresults

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.msafonov.swimresults.auth.AuthState
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.viewmodel.AthleteFormError
import com.msafonov.swimresults.viewmodel.AthleteFormViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AthleteFormViewModelTest {
    @Test fun sameAccountDraftRestoresAndEmissionsNeverOverwriteEditsThenLogoutRedactsIt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val dao = InMemoryProfileDao()
            val profiles = profileFixture(dao)
            val id = profiles.save(profiles.session.current, null, "Исходное", "02.03.2009", null)
            val saved = SavedStateHandle()
            val first = AthleteFormViewModel(profiles, saved).also { store.put("form", it) }
            first.initialize(id); runCurrent()
            first.name("Черновик"); first.birthDate("29.02.2012"); first.gender(Swimmer.Women); first.rank(SwimmingRank.First)
            dao.changed(); first.initialize(id); runCurrent()
            assertEquals("Черновик", first.uiState.value.name)
            assertTrue(first.uiState.value.dirty)
            val recreated = AthleteFormViewModel(profiles, saved).also { store.put("form", it) }
            recreated.initialize(id); runCurrent()
            assertEquals("Черновик", recreated.uiState.value.name)
            assertEquals(Swimmer.Women, recreated.uiState.value.gender)
            assertEquals("29.02.2012", recreated.uiState.value.birthDate)
            assertEquals(SwimmingRank.First, recreated.uiState.value.rank)
            recreated.rank(null); assertTrue(recreated.uiState.value.dirty)
            recreated.rank(SwimmingRank.First)
            profiles.session.change(AuthState.Guest())
            runCurrent()
            assertEquals("", recreated.uiState.value.name)
            assertEquals("", recreated.uiState.value.birthDate)
            assertNull(recreated.uiState.value.rank)
            assertNull(saved.get<String>("draftBirthDate"))
            assertNull(saved.get<String>("draftRank"))
            assertFalse(recreated.uiState.value.validSession)
            assertEquals(AthleteFormError.SessionChanged, recreated.uiState.value.error)
            recreated.save { error("Stale save") }
            signedIn(profiles.session, "B"); profiles.current()
            val other = AthleteFormViewModel(profiles, saved).also { store.put("other", it) }
            other.initialize(null); runCurrent()
            assertEquals("", other.uiState.value.name)
            assertNull(other.uiState.value.gender)
            assertNull(other.uiState.value.rank)
            assertEquals("", other.uiState.value.birthDate)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun writeFailureKeepsDraftAndCancellationPropagatesWithoutSavingOrNavigating() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val dao = InMemoryProfileDao()
            val profiles = profileFixture(dao)
            val vm = AthleteFormViewModel(profiles, SavedStateHandle()).also { store.put("form", it) }
            vm.initialize(null); runCurrent()
            vm.save { error("Invalid save navigated") }; runCurrent()
            assertEquals(AthleteFormError.InvalidInput, vm.uiState.value.error)
            vm.name("Имя")
            assertTrue(vm.uiState.value.valid)
            vm.birthDate("29.02.2023")
            assertFalse(vm.uiState.value.valid)
            vm.save { error("Invalid date navigated") }; runCurrent()
            assertTrue(dao.athletes.isEmpty())
            vm.birthDate("29.02.2012"); vm.rank(SwimmingRank.SecondJunior)
            assertTrue(vm.uiState.value.valid)
            dao.beforeWrite = { throw IllegalStateException("disk") }
            vm.save { error("Failed save navigated") }; runCurrent()
            assertEquals("Имя", vm.uiState.value.name)
            assertEquals(AthleteFormError.SaveFailed, vm.uiState.value.error)
            assertFalse(vm.uiState.value.saving)
            dao.beforeWrite = { throw CancellationException("cancel") }
            vm.save { error("Cancelled save navigated") }; runCurrent()
            assertNull(vm.uiState.value.error)
            assertTrue(dao.athletes.isEmpty())
            assertFalse(vm.uiState.value.saving)
            dao.beforeWrite = {}
            var saves = 0
            vm.save { saves++ }; vm.save { saves++ }; runCurrent()
            assertEquals(1, saves)
            assertEquals(1, dao.athletes.size)
            assertEquals("2012-02-29", dao.athletes.values.single().birthDate)
            assertEquals("SecondJunior", dao.athletes.values.single().rank)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun editingEveryFieldBackToOriginalClearsDirtyAndOldYearDraftIsIgnored() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val profiles = profileFixture()
            val id = profiles.save(profiles.session.current, null, "Имя", "01.02.2000", Swimmer.Men, SwimmingRank.Third)
            val saved = SavedStateHandle(mapOf("draftYear" to "1999"))
            val vm = AthleteFormViewModel(profiles, saved).also { store.put("form", it) }
            vm.initialize(id); runCurrent()
            assertEquals("01.02.2000", vm.uiState.value.birthDate)
            assertFalse(vm.uiState.value.dirty)
            vm.birthDate(""); vm.rank(null); vm.gender(null); vm.name("Другое")
            assertTrue(vm.uiState.value.dirty)
            vm.birthDate("01.02.2000"); vm.rank(SwimmingRank.Third); vm.gender(Swimmer.Men); vm.name("Имя")
            assertFalse(vm.uiState.value.dirty)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }
}
