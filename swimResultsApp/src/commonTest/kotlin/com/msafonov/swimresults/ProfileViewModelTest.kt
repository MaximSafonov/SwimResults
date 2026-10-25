package com.msafonov.swimresults

import androidx.lifecycle.ViewModelStore
import com.msafonov.swimresults.auth.AccountSession
import com.msafonov.swimresults.auth.AuthState
import com.msafonov.swimresults.data.repository.ProfileRepository
import com.msafonov.swimresults.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @Test fun guestAndAccountWithoutAthletesNeverCreateAnAthleteAndLogoutHidesAccount() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val session = AccountSession()
            val dao = InMemoryProfileDao()
            val profiles = ProfileRepository(dao, session)
            val vm = ProfileViewModel(profiles, TestAuth(session)).also { store.put("profile", it) }
            runCurrent()
            assertIs<AuthState.Guest>(vm.session.value.auth)
            assertTrue(dao.athletes.isEmpty())
            signedIn(session)
            runCurrent()
            assertEquals("vk:A", vm.uiState.value.profile.session.accountId)
            assertTrue(vm.uiState.value.profile.athletes.isEmpty())
            profiles.save(session.current, null, "Спортсмен", "", null)
            runCurrent()
            assertEquals(1, vm.uiState.value.profile.athletes.size)
            vm.logout()
            runCurrent()
            assertNull(vm.uiState.value.profile.session.accountId)
            assertTrue(vm.uiState.value.profile.athletes.isEmpty())
            assertEquals(1, dao.athletes.size)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }
}
