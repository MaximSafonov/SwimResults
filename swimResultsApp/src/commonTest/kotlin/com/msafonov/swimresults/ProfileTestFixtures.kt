package com.msafonov.swimresults

import com.msafonov.swimresults.auth.*
import com.msafonov.swimresults.data.local.dao.ProfileDao
import com.msafonov.swimresults.data.local.entity.AccountEntity
import com.msafonov.swimresults.data.local.entity.AthleteEntity
import com.msafonov.swimresults.data.repository.ProfileRepository
import com.msafonov.swimresults.data.storage.AuthSessionPolicyStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll

/** Test source only. Not a substitute for Room transaction tests. */
internal class InMemoryProfileDao : ProfileDao() {
    val accounts = mutableMapOf<String, AccountEntity>()
    val athletes = mutableMapOf<Long, AthleteEntity>()
    private val version = MutableStateFlow(0)
    private var nextId = 1L
    var beforeWrite: suspend () -> Unit = {}
    var beforeActiveWrite: suspend () -> Unit = {}
    var activeWrites = 0
        private set
    var athleteCollections = 0
        private set
    fun changed() { version.value++ }
    override fun observeAccount(accountId: String) = version.map { accounts[accountId] }
    override fun observeAthletes(accountId: String) = flow {
        athleteCollections++
        emitAll(version.map { athletes.values.filter { it.accountId == accountId }.sortedBy { it.id } })
    }
    override suspend fun getAccount(accountId: String) = accounts[accountId]
    override suspend fun ownedAthlete(accountId: String, id: Long) = athletes[id]?.takeIf { it.accountId == accountId }
    override suspend fun insertAccount(account: AccountEntity): Long {
        if (account.id in accounts) return -1
        accounts[account.id] = account
        changed()
        return accounts.size.toLong()
    }
    override suspend fun updateAccountName(accountId: String, name: String) {
        val current = accounts[accountId] ?: return
        if (current.displayName != name) { accounts[accountId] = current.copy(displayName = name); changed() }
    }
    override suspend fun insertAthlete(athlete: AthleteEntity): Long {
        beforeWrite()
        val id = nextId++
        athletes[id] = athlete.copy(id = id)
        changed()
        return id
    }
    override suspend fun updateAthlete(accountId: String, id: Long, name: String, birthDate: String?, gender: String?, rank: String?): Int {
        beforeWrite()
        val current = ownedAthlete(accountId, id) ?: return 0
        athletes[id] = current.copy(name = name, birthDate = birthDate, sportsGender = gender, rank = rank)
        changed()
        return 1
    }
    override suspend fun updateActive(accountId: String, athleteId: Long): Int {
        activeWrites++
        beforeActiveWrite()
        val current = accounts[accountId] ?: return 0
        accounts[accountId] = current.copy(activeAthleteId = athleteId)
        changed()
        return 1
    }
}

internal class TestAuthPolicy : AuthSessionPolicyStorage {
    var allowed = false
    var failure: Exception? = null
    override suspend fun restoreAllowed() = allowed
    override suspend fun setRestoreAllowed(allowed: Boolean) { failure?.let { throw it }; this.allowed = allowed }
}

internal class TestAuth(private val session: AccountSession) : AuthSession {
    override suspend fun restore() { if (session.current.auth == AuthState.Restoring) session.change(AuthState.Guest()) }
    override suspend fun logout() { session.change(AuthState.Guest()) }
}

internal suspend fun signedIn(session: AccountSession, user: String = "A") {
    session.change(AuthState.Authenticated(AccountPrincipal(user, "Account $user")))
}

internal suspend fun profileFixture(dao: InMemoryProfileDao = InMemoryProfileDao(), session: AccountSession = AccountSession()): ProfileRepository {
    signedIn(session)
    return ProfileRepository(dao, session).also { it.current() }
}

internal class TestPreferencesStore(initial: androidx.datastore.preferences.core.Preferences) : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
    private val mutable = MutableStateFlow(initial)
    private val mutex = kotlinx.coroutines.sync.Mutex()
    override val data: kotlinx.coroutines.flow.Flow<androidx.datastore.preferences.core.Preferences> = mutable
    override suspend fun updateData(transform: suspend (t: androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences): androidx.datastore.preferences.core.Preferences {
        mutex.lock()
        try { return transform(mutable.value).also { mutable.value = it } }
        finally { mutex.unlock() }
    }
}
