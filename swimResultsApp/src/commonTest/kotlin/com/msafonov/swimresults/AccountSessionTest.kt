package com.msafonov.swimresults

import com.msafonov.swimresults.auth.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSessionTest {
    @Test fun restorePolicyDefaultsToSuppressedAndNeverChangesThemePreference() = runTest {
        val themeKey = androidx.datastore.preferences.core.booleanPreferencesKey("is_dark")
        val store = TestPreferencesStore(androidx.datastore.preferences.core.preferencesOf(themeKey to true))
        val owner = com.msafonov.swimresults.data.local.SettingsDataStoreOwner(store, backgroundScope)
        val policy = com.msafonov.swimresults.data.storage.DataStoreAuthSessionPolicyStorage(owner)
        val theme = com.msafonov.swimresults.data.storage.DataStoreSettingsStorage(owner)
        assertFalse(policy.restoreAllowed())
        policy.setRestoreAllowed(true)
        assertTrue(policy.restoreAllowed())
        policy.setRestoreAllowed(false)
        assertFalse(policy.restoreAllowed())
        assertTrue(theme.isDark.first())
    }

    @Test fun staleCallbacksAndOperationsCannotReopenAfterLogoutOrAccountSwitch() = runTest {
        val session = AccountSession()
        signedIn(session)
        val old = session.current
        session.change(AuthState.Guest())
        assertFalse(session.change(AuthState.Authenticated(AccountPrincipal("A", "Late")), old))
        assertFailsWith<StaleSessionException> { session.guard(old) { error("Old operation executed") } }
        signedIn(session, "B")
        assertEquals("vk:B", session.current.accountId)
        assertFalse(session.change(AuthState.Guest(), old))
    }

    @Test fun logoutPublicationWaitsForPriorLocalCommitAndRejectsEveryLaterOldCommit() = runTest {
        val session = AccountSession()
        signedIn(session)
        val old = session.current
        val gate = CompletableDeferred<Unit>()
        val order = mutableListOf<String>()
        val write = launch { session.guard(old) { gate.await(); order += "commit" } }
        runCurrent()
        val logout = async { session.change(AuthState.Guest()).also { order += "logout" } }
        runCurrent()
        assertFalse(logout.isCompleted)
        gate.complete(Unit)
        write.join(); logout.await()
        assertEquals(listOf("commit", "logout"), order)
        assertFailsWith<StaleSessionException> { session.guard(old) { order += "late" } }
    }

    @Test fun failedDurableSuppressionDoesNotAnnounceLogoutAndSuccessPersistsOnlyBoolean() = runTest {
        val session = AccountSession()
        signedIn(session)
        val policy = TestAuthPolicy().also { it.allowed = true; it.failure = IllegalStateException("disk") }
        val old = session.current
        assertFailsWith<IllegalStateException> { session.change(AuthState.Guest(), old) { policy.setRestoreAllowed(false) } }
        assertEquals(old, session.current)
        assertTrue(policy.allowed)
        policy.failure = null
        assertTrue(session.change(AuthState.Guest(), old) { policy.setRestoreAllowed(false) })
        assertFalse(policy.restoreAllowed())
        assertNull(session.current.accountId)
    }
}
