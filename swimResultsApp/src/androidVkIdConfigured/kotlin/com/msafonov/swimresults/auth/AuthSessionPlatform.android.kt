package com.msafonov.swimresults.auth

import android.content.Context
import org.jetbrains.compose.resources.stringResource
import swimresults.swimresultsapp.generated.resources.Res
import swimresults.swimresultsapp.generated.resources.authWidgetError
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msafonov.swimresults.data.local.getSettingsDataStoreOwner
import com.msafonov.swimresults.data.storage.AuthSessionPolicyStorage
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.VKIDAuthUiParams
import com.vk.id.logout.VKIDLogoutCallback
import com.vk.id.logout.VKIDLogoutFail
import com.vk.id.onetap.compose.onetap.OneTap
import com.vk.id.onetap.compose.onetap.OneTapTitleScenario
import com.vk.id.refresh.VKIDRefreshTokenCallback
import com.vk.id.refresh.VKIDRefreshTokenFail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

private object AndroidAuthContext {
    var context: Context? = null
    private var initialized = false
    @Synchronized fun sdk(): VKID {
        if (!initialized) {
            VKID.logsEnabled = false
            VKID.init(checkNotNull(context))
            initialized = true
        }
        return VKID.instance
    }
}

@Composable
actual fun InitializeAuthPlatform() {
    AndroidAuthContext.context = LocalContext.current.applicationContext
}

actual fun createAuthSession(session: AccountSession, policy: AuthSessionPolicyStorage): AuthSession = AndroidVkAuthSession(session, policy)

private sealed interface RefreshOutcome {
    data class Success(val token: AccessToken) : RefreshOutcome
    data class Failed(val error: AuthError) : RefreshOutcome
}

private class AndroidVkAuthSession(val session: AccountSession, private val policy: AuthSessionPolicyStorage) : AuthSession {
    private val operations = Mutex()
    private val scope = getSettingsDataStoreOwner().scope
    private var restored = false
    private var expiryJob: Job? = null

    fun initializeWidget(): Boolean = try { AndroidAuthContext.sdk(); true } catch (_: Exception) { false }

    override suspend fun restore() = operations.withLock {
        if (restored) return@withLock
        restored = true
        val expected = session.current
        try {
            if (!policy.restoreAllowed()) { session.change(AuthState.Guest(), expected); return@withLock }
            val cached = AndroidAuthContext.sdk().accessToken
            if (cached == null) { session.change(AuthState.ReauthenticationRequired(AuthError.Expired), expected); return@withLock }
            if (valid(cached)) acceptLocked(cached, expected, false)
            else refreshLocked(expected, cached.userID)
        } catch (cancelled: CancellationException) { restored = false; throw cancelled }
        catch (_: Exception) { session.change(AuthState.Guest(AuthError.Storage), expected) }
    }

    private fun valid(token: AccessToken): Boolean = token.userID > 0 && (token.expireTime == -1L || token.expireTime > System.currentTimeMillis())

    private suspend fun acceptLocked(token: AccessToken, expected: SessionSnapshot, permitRestore: Boolean) {
        if (!valid(token)) { session.change(AuthState.ReauthenticationRequired(AuthError.Expired), expected); return }
        val name = listOf(token.userData.firstName, token.userData.lastName).filter { it.isNotBlank() }.joinToString(" ")
        val changed = session.change(AuthState.Authenticated(AccountPrincipal(token.userID.toString(), name)), expected) {
            if (permitRestore) policy.setRestoreAllowed(true)
        }
        if (!changed) return
        expiryJob?.cancel()
        val confirmed = session.current
        if (token.expireTime != -1L) expiryJob = scope.launch {
            delay((token.expireTime - System.currentTimeMillis()).coerceAtLeast(1L))
            operations.withLock {
                if (!session.change(AuthState.Restoring, confirmed)) return@withLock
                refreshLocked(session.current, token.userID)
            }
        }
    }

    private suspend fun refreshLocked(expected: SessionSnapshot, userId: Long) {
        try {
            val outcome = withTimeout(15_000) {
                val result = CompletableDeferred<RefreshOutcome>()
                AndroidAuthContext.sdk().refreshToken(object : VKIDRefreshTokenCallback {
                    override fun onSuccess(token: AccessToken) { result.complete(RefreshOutcome.Success(token)) }
                    override fun onFail(fail: VKIDRefreshTokenFail) {
                        val error = when (fail) {
                            is VKIDRefreshTokenFail.NotAuthenticated, is VKIDRefreshTokenFail.RefreshTokenExpired -> AuthError.Expired
                            is VKIDRefreshTokenFail.FailedApiCall -> AuthError.Network
                            else -> AuthError.Provider
                        }
                        result.complete(RefreshOutcome.Failed(error))
                    }
                })
                result.await()
            }
            when (outcome) {
                is RefreshOutcome.Success -> if (outcome.token.userID == userId) acceptLocked(outcome.token, expected, false)
                    else session.change(AuthState.ReauthenticationRequired(AuthError.Provider), expected)
                is RefreshOutcome.Failed -> session.change(AuthState.ReauthenticationRequired(outcome.error), expected)
            }
        } catch (_: TimeoutCancellationException) { session.change(AuthState.ReauthenticationRequired(AuthError.Network), expected) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { session.change(AuthState.ReauthenticationRequired(AuthError.Network), expected) }
    }

    fun authenticated(token: AccessToken, expected: SessionSnapshot) {
        scope.launch { operations.withLock {
            try { acceptLocked(token, expected, true) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { session.change(AuthState.Guest(AuthError.Storage), expected) }
        } }
    }

    fun failed(fail: VKIDAuthFail, expected: SessionSnapshot) {
        if (fail is VKIDAuthFail.Canceled) return
        scope.launch { operations.withLock { session.change(AuthState.Guest(AuthError.Provider), expected) } }
    }

    override suspend fun logout() = operations.withLock {
        val expected = session.current
        // A durable suppression write must succeed before announcing local logout.
        if (!session.change(AuthState.Guest(), expected) { policy.setRestoreAllowed(false) }) return@withLock
        expiryJob?.cancel()
        expiryJob = null
        val loggedOut = session.current
        try {
            val success = withTimeout(15_000) {
                val result = CompletableDeferred<Boolean>()
                AndroidAuthContext.sdk().logout(object : VKIDLogoutCallback {
                    override fun onSuccess() { result.complete(true) }
                    override fun onFail(fail: VKIDLogoutFail) { result.complete(false) }
                })
                result.await()
            }
            if (!success) session.change(AuthState.Guest(AuthError.LogoutUnconfirmed), loggedOut)
        } catch (_: TimeoutCancellationException) { session.change(AuthState.Guest(AuthError.LogoutUnconfirmed), loggedOut) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { session.change(AuthState.Guest(AuthError.LogoutUnconfirmed), loggedOut) }
    }
}

@Composable
actual fun AuthSignIn(auth: AuthSession) {
    val adapter = auth as AndroidVkAuthSession
    val snapshot by adapter.session.state.collectAsStateWithLifecycle()
    val ready = remember(adapter) { adapter.initializeWidget() }
    if (!ready) { Text(stringResource(Res.string.authWidgetError)); return }
    key(snapshot.epoch) {
        val expected = snapshot
        OneTap(modifier = Modifier.fillMaxWidth(), scenario = OneTapTitleScenario.SignIn,
            fastAuthEnabled = false, signInAnotherAccountButtonEnabled = true,
            authParams = VKIDAuthUiParams { scopes = emptySet() },
            onAuth = { _, token -> adapter.authenticated(token, expected) },
            onFail = { _, failure -> adapter.failed(failure, expected) })
    }
}
