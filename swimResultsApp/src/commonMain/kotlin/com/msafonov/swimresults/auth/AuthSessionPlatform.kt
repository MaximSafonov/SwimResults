package com.msafonov.swimresults.auth

import androidx.compose.runtime.Composable
import com.msafonov.swimresults.data.storage.AuthSessionPolicyStorage

@Composable
expect fun InitializeAuthPlatform()

expect fun createAuthSession(session: AccountSession, policy: AuthSessionPolicyStorage): AuthSession

/** Public provider-owned sign-in widget. Rendering it must not launch authorization. */
@Composable
expect fun AuthSignIn(auth: AuthSession)
