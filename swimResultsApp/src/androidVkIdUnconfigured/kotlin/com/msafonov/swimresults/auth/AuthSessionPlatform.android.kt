package com.msafonov.swimresults.auth

import androidx.compose.runtime.Composable
import com.msafonov.swimresults.data.storage.AuthSessionPolicyStorage

@Composable
actual fun InitializeAuthPlatform() = Unit

actual fun createAuthSession(session: AccountSession, policy: AuthSessionPolicyStorage): AuthSession = object : AuthSession {
    override suspend fun restore() {
        val snapshot = session.current
        if (snapshot.auth == AuthState.Restoring) session.change(AuthState.NotConfigured, snapshot)
    }
    override suspend fun logout() = Unit
}

@Composable
actual fun AuthSignIn(auth: AuthSession) = Unit
