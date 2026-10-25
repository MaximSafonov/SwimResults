package com.msafonov.swimresults.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

data class SessionSnapshot(val epoch: Long = 0, val auth: AuthState = AuthState.Restoring) {
    val principal: AccountPrincipal? get() = (auth as? AuthState.Authenticated)?.principal
    val accountId: String? get() = principal?.accountId
}

class StaleSessionException : IllegalStateException("Account session changed")

/** Serializes short local commits and session publication. Never hold across provider networking. */
@Single
class AccountSession {
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(SessionSnapshot())
    val state = mutable.asStateFlow()
    val current: SessionSnapshot get() = mutable.value

    suspend fun change(auth: AuthState, expected: SessionSnapshot? = null, beforePublication: suspend () -> Unit = {}): Boolean = mutex.withLock {
        if (expected != null && current != expected) return@withLock false
        beforePublication()
        mutable.value = SessionSnapshot(current.epoch + 1, auth)
        true
    }

    suspend fun <T> guard(snapshot: SessionSnapshot, block: suspend () -> T): T = mutex.withLock {
        if (current != snapshot) throw StaleSessionException()
        block()
    }

    fun isCurrent(snapshot: SessionSnapshot): Boolean = current == snapshot
}
