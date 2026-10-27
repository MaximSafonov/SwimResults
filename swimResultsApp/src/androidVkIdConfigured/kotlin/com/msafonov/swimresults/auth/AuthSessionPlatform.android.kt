package com.msafonov.swimresults.auth

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.util.Log
import org.jetbrains.compose.resources.stringResource
import swimresults.swimresultsapp.generated.resources.Res
import swimresults.swimresultsapp.generated.resources.authWidgetError
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

private enum class AuthEvent {
    INIT_SUCCESS, INIT_FAILURE, FLOW_READY, FLOW_DISPOSED, CODE_RECEIVED,
    CALLBACK_AUTH, CALLBACK_FAIL, CALLBACK_ENQUEUED, CALLBACK_LOCKED,
    RESTORE_SUPPRESSED, RESTORE_CACHE_ABSENT, RESTORE_CACHE_VALID, RESTORE_REFRESH,
    RESTORE_FAILURE, TOKEN_INVALID, STORAGE_FAILURE, TRANSITION,
    REFRESH_START, REFRESH_SUCCESS, REFRESH_FAIL, REFRESH_ACCOUNT_MISMATCH, REFRESH_TIMEOUT, REFRESH_FAILURE,
    RATE_LIMITED,
}

private enum class AuthOutcome { NONE, APPLIED, STALE_REJECTED }
private enum class AuthFailure { NONE, CANCELED, API, OAUTH, STATE, REDIRECT, NO_BROWSER, EXPIRED, NOT_AUTHENTICATED, OTHER }
private enum class ThrowableCategory { NONE, DNS, TIMEOUT, TLS, CONNECT, IO, SECURITY, STATE, ARGUMENT, OTHER }
private enum class AuthStatus { NONE, NOT_CONFIGURED, UNSUPPORTED, RESTORING, GUEST, AUTHENTICATED, REAUTHENTICATION_REQUIRED }
private data class AuthTrace(val flow: Long = 0, val callback: Long = 0, val refresh: Long = 0)
private val noAuthTrace = AuthTrace()

// All fields are local counters or enums. No SDK payload or arbitrary text crosses this boundary.
private object AuthDiagnostics {
    private var enabled = false
    private var startedAt = 0L
    private var windowStart = 0L
    private var lines = 0
    private var sequence = 0L
    private var flows = 0L
    private var callbacks = 0L
    private var refreshes = 0L

    @Synchronized
    fun configure(context: Context) {
        try {
            enabled = context.applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            if (enabled) {
                startedAt = SystemClock.elapsedRealtime()
                windowStart = startedAt
            }
        } catch (_: Throwable) { enabled = false }
    }

    @Synchronized
    fun newFlow(): AuthTrace = try {
        if (!enabled) noAuthTrace else {
            flows = increment(flows)
            AuthTrace(flow = flows)
        }
    } catch (_: Throwable) { noAuthTrace }

    @Synchronized
    fun newRefreshAttempt(trace: AuthTrace): AuthTrace = try {
        if (!enabled) noAuthTrace else {
            refreshes = increment(refreshes)
            trace.copy(refresh = refreshes)
        }
    } catch (_: Throwable) { noAuthTrace }

    @Synchronized
    fun callback(
        event: AuthEvent,
        flow: AuthTrace,
        expected: SessionSnapshot,
        current: SessionSnapshot,
        failure: AuthFailure = AuthFailure.NONE,
        throwable: Throwable? = null,
        completion: Boolean? = null,
        diagnostics: RefreshFailureDiagnostics? = null,
    ): AuthTrace = try {
        if (!enabled) noAuthTrace else {
            callbacks = increment(callbacks)
            val trace = AuthTrace(flow.flow, callbacks)
            record(event, trace, expected, current, failure = failure, throwable = throwable, completion = completion,
                refreshDiagnostics = diagnostics)
            trace
        }
    } catch (_: Throwable) { noAuthTrace }

    @Synchronized
    fun record(
        event: AuthEvent,
        trace: AuthTrace = noAuthTrace,
        expected: SessionSnapshot? = null,
        current: SessionSnapshot? = null,
        target: AuthState? = null,
        outcome: AuthOutcome = AuthOutcome.NONE,
        failure: AuthFailure = AuthFailure.NONE,
        throwable: Throwable? = null,
        completion: Boolean? = null,
        refreshDiagnostics: RefreshFailureDiagnostics? = null,
    ) {
        // Diagnostics must never change an SDK callback, coroutine or session result.
        try {
            if (!enabled) return
            val now = SystemClock.elapsedRealtime()
            if (now - windowStart >= 60_000L) {
                windowStart = now
                lines = 0
            }
            if (lines >= 200) return
            val limited = lines == 199
            lines++
            sequence = increment(sequence)
            val message = buildString {
                append("seq=").append(sequence)
                append(" t_ms=").append(now - startedAt)
                append(" event=").append(if (limited) AuthEvent.RATE_LIMITED else event)
                if (!limited) {
                    append(" flow=").append(trace.flow).append(" cb=").append(trace.callback)
                    if (trace.refresh != 0L) append(" refresh=").append(trace.refresh)
                    if (refreshDiagnostics != null) {
                        append(" stage=").append(refreshDiagnostics.stage)
                        append(" http=").append(refreshDiagnostics.httpStatus ?: "UNAVAILABLE")
                        append(" api=").append(refreshDiagnostics.apiError)
                    }
                    if (expected != null) append(" expected_epoch=").append(expected.epoch)
                    if (current != null) {
                        append(" current_epoch=").append(current.epoch)
                        append(" status=").append(status(current.auth))
                    }
                    if (outcome != AuthOutcome.NONE) {
                        append(" outcome=").append(outcome)
                        append(" from=").append(status(expected?.auth))
                        append(" to=").append(status(target))
                        error(target)?.let { append(" error=").append(it) }
                    }
                    if (failure != AuthFailure.NONE) append(" fail=").append(failure)
                    if (throwable != null || failure != AuthFailure.NONE) append(" throwable=").append(category(throwable))
                    if (completion != null) append(" completion=").append(completion)
                }
            }
            Log.i("SwimResultsAuth", message.take(320))
        } catch (_: Throwable) { /* Logging failures are observational only. */ }
    }

    private fun increment(value: Long): Long = if (value == Long.MAX_VALUE) value else value + 1

    private fun status(state: AuthState?): AuthStatus = when (state) {
        null -> AuthStatus.NONE
        AuthState.NotConfigured -> AuthStatus.NOT_CONFIGURED
        AuthState.Unsupported -> AuthStatus.UNSUPPORTED
        AuthState.Restoring -> AuthStatus.RESTORING
        is AuthState.Guest -> AuthStatus.GUEST
        is AuthState.Authenticated -> AuthStatus.AUTHENTICATED
        is AuthState.ReauthenticationRequired -> AuthStatus.REAUTHENTICATION_REQUIRED
    }

    private fun error(state: AuthState?): AuthError? = when (state) {
        is AuthState.Guest -> state.error
        is AuthState.ReauthenticationRequired -> state.error
        else -> null
    }

    private fun category(throwable: Throwable?): ThrowableCategory {
        if (throwable == null) return ThrowableCategory.NONE
        var cause: Throwable? = throwable
        var fallback = ThrowableCategory.OTHER
        repeat(4) {
            val current = cause ?: return fallback
            when (current) {
                is UnknownHostException -> return ThrowableCategory.DNS
                is SocketTimeoutException -> return ThrowableCategory.TIMEOUT
                is SSLException -> return ThrowableCategory.TLS
                is ConnectException -> return ThrowableCategory.CONNECT
                is IOException -> return ThrowableCategory.IO
                is SecurityException -> return ThrowableCategory.SECURITY
                is IllegalArgumentException -> return ThrowableCategory.ARGUMENT
                is IllegalStateException -> fallback = ThrowableCategory.STATE
            }
            cause = current.cause
        }
        return fallback
    }
}

private object AndroidAuthContext {
    private var initializationResult: Boolean? = null

    @Synchronized
    fun initialize(context: Context): Boolean {
        initializationResult?.let { return it }
        AuthDiagnostics.configure(context)
        val initialized = try {
            VKID.logsEnabled = false
            VKID.init(context.applicationContext)
            AuthDiagnostics.record(AuthEvent.INIT_SUCCESS)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            AuthDiagnostics.record(AuthEvent.INIT_FAILURE, throwable = failure)
            false
        }
        // Do not repeat a possibly partial SDK initialization in the same process.
        initializationResult = initialized
        return initialized
    }

    @Synchronized
    fun sdk(): VKID {
        check(initializationResult == true) { "VK ID initialization unavailable" }
        return VKID.instance
    }
}

internal fun initializeVkId(context: Context): Boolean = AndroidAuthContext.initialize(context)

@Composable
actual fun InitializeAuthPlatform() {
    initializeVkId(LocalContext.current.applicationContext)
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

    private suspend fun transition(
        target: AuthState,
        expected: SessionSnapshot,
        trace: AuthTrace = noAuthTrace,
        beforePublication: suspend () -> Unit = {},
    ): Boolean {
        val applied = session.change(target, expected, beforePublication)
        AuthDiagnostics.record(AuthEvent.TRANSITION, trace, expected, session.current, target,
            if (applied) AuthOutcome.APPLIED else AuthOutcome.STALE_REJECTED)
        return applied
    }

    override suspend fun restore() = operations.withLock {
        if (restored) return@withLock
        restored = true
        val expected = session.current
        try {
            if (!policy.restoreAllowed()) {
                AuthDiagnostics.record(AuthEvent.RESTORE_SUPPRESSED, expected = expected, current = session.current)
                transition(AuthState.Guest(), expected)
                return@withLock
            }
            val cached = AndroidAuthContext.sdk().accessToken
            if (cached == null) {
                AuthDiagnostics.record(AuthEvent.RESTORE_CACHE_ABSENT, expected = expected, current = session.current)
                transition(AuthState.ReauthenticationRequired(AuthError.Expired), expected)
                return@withLock
            }
            if (valid(cached)) {
                AuthDiagnostics.record(AuthEvent.RESTORE_CACHE_VALID, expected = expected, current = session.current)
                acceptLocked(cached, expected, false)
            } else {
                AuthDiagnostics.record(AuthEvent.RESTORE_REFRESH, expected = expected, current = session.current)
                refreshLocked(expected, cached.userID)
            }
        } catch (cancelled: CancellationException) { restored = false; throw cancelled }
        catch (failure: Exception) {
            AuthDiagnostics.record(AuthEvent.RESTORE_FAILURE, expected = expected, current = session.current, throwable = failure)
            transition(AuthState.Guest(AuthError.Storage), expected)
        }
    }

    private fun valid(token: AccessToken): Boolean = token.userID > 0 && (token.expireTime == -1L || token.expireTime > System.currentTimeMillis())

    private suspend fun acceptLocked(token: AccessToken, expected: SessionSnapshot, permitRestore: Boolean, trace: AuthTrace = noAuthTrace) {
        if (!valid(token)) {
            AuthDiagnostics.record(AuthEvent.TOKEN_INVALID, trace, expected, session.current)
            transition(AuthState.ReauthenticationRequired(AuthError.Expired), expected, trace)
            return
        }
        val name = listOf(token.userData.firstName, token.userData.lastName).filter { it.isNotBlank() }.joinToString(" ")
        val changed = transition(AuthState.Authenticated(AccountPrincipal(token.userID.toString(), name)), expected, trace) {
            if (permitRestore) policy.setRestoreAllowed(true)
        }
        if (!changed) return
        expiryJob?.cancel()
        val confirmed = session.current
        if (token.expireTime != -1L) expiryJob = scope.launch {
            delay((token.expireTime - System.currentTimeMillis()).coerceAtLeast(1L))
            operations.withLock {
                if (!transition(AuthState.Restoring, confirmed, trace)) return@withLock
                refreshLocked(session.current, token.userID, trace)
            }
        }
    }

    private suspend fun refreshLocked(expected: SessionSnapshot, userId: Long, trace: AuthTrace = noAuthTrace) {
        val refreshTrace = AuthDiagnostics.newRefreshAttempt(trace)
        AuthDiagnostics.record(AuthEvent.REFRESH_START, refreshTrace, expected, session.current)
        try {
            val outcome = withTimeout(15_000) {
                val result = CompletableDeferred<RefreshOutcome>()
                AndroidAuthContext.sdk().refreshToken(object : VKIDRefreshTokenCallback {
                    override fun onSuccess(token: AccessToken) { result.complete(RefreshOutcome.Success(token)) }
                    override fun onFail(fail: VKIDRefreshTokenFail) {
                        val diagnostics = try {
                            val failedApiCall = fail as? VKIDRefreshTokenFail.FailedApiCall
                            RefreshFailureDiagnostics.parse(failedApiCall?.description, failedApiCall?.throwable?.message)
                        } catch (_: Throwable) {
                            RefreshFailureDiagnostics(RefreshStage.UNKNOWN, null, RefreshApiError.UNAVAILABLE)
                        }
                        AuthDiagnostics.record(AuthEvent.REFRESH_FAIL, refreshTrace, expected, session.current,
                            failure = when (fail) {
                                is VKIDRefreshTokenFail.NotAuthenticated -> AuthFailure.NOT_AUTHENTICATED
                                is VKIDRefreshTokenFail.RefreshTokenExpired -> AuthFailure.EXPIRED
                                is VKIDRefreshTokenFail.FailedApiCall -> AuthFailure.API
                                is VKIDRefreshTokenFail.FailedOAuthState -> AuthFailure.STATE
                                else -> AuthFailure.OTHER
                            },
                            throwable = (fail as? VKIDRefreshTokenFail.FailedApiCall)?.throwable,
                            refreshDiagnostics = diagnostics)
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
                is RefreshOutcome.Success -> if (outcome.token.userID == userId) {
                    AuthDiagnostics.record(AuthEvent.REFRESH_SUCCESS, refreshTrace, expected, session.current)
                    acceptLocked(outcome.token, expected, false, refreshTrace)
                } else {
                    AuthDiagnostics.record(AuthEvent.REFRESH_ACCOUNT_MISMATCH, refreshTrace, expected, session.current)
                    transition(AuthState.ReauthenticationRequired(AuthError.Provider), expected, refreshTrace)
                }
                is RefreshOutcome.Failed -> transition(AuthState.ReauthenticationRequired(outcome.error), expected, refreshTrace)
            }
        } catch (failure: TimeoutCancellationException) {
            AuthDiagnostics.record(AuthEvent.REFRESH_TIMEOUT, refreshTrace, expected, session.current, throwable = failure)
            transition(AuthState.ReauthenticationRequired(AuthError.Network), expected, refreshTrace)
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            AuthDiagnostics.record(AuthEvent.REFRESH_FAILURE, refreshTrace, expected, session.current, throwable = failure)
            transition(AuthState.ReauthenticationRequired(AuthError.Network), expected, refreshTrace)
        }
    }

    fun codeReceived(flow: AuthTrace, expected: SessionSnapshot, completion: Boolean) {
        AuthDiagnostics.callback(AuthEvent.CODE_RECEIVED, flow, expected, session.current, completion = completion)
    }

    fun authenticated(token: AccessToken, expected: SessionSnapshot, flow: AuthTrace) {
        val trace = AuthDiagnostics.callback(AuthEvent.CALLBACK_AUTH, flow, expected, session.current)
        AuthDiagnostics.record(AuthEvent.CALLBACK_ENQUEUED, trace, expected, session.current)
        scope.launch { operations.withLock {
            AuthDiagnostics.record(AuthEvent.CALLBACK_LOCKED, trace, expected, session.current)
            try { acceptLocked(token, expected, true, trace) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                AuthDiagnostics.record(AuthEvent.STORAGE_FAILURE, trace, expected, session.current, throwable = failure)
                transition(AuthState.Guest(AuthError.Storage), expected, trace)
            }
        } }
    }

    fun failed(fail: VKIDAuthFail, expected: SessionSnapshot, flow: AuthTrace) {
        val diagnostics = (fail as? VKIDAuthFail.FailedApiCall)?.let { failedApiCall ->
            try {
                RefreshFailureDiagnostics.parseInteractive(failedApiCall.description, failedApiCall.throwable)
            } catch (_: Throwable) {
                RefreshFailureDiagnostics(RefreshStage.UNKNOWN, null, RefreshApiError.UNAVAILABLE)
            }
        }
        val trace = AuthDiagnostics.callback(AuthEvent.CALLBACK_FAIL, flow, expected, session.current,
            failure = when (fail) {
                is VKIDAuthFail.Canceled -> AuthFailure.CANCELED
                is VKIDAuthFail.FailedApiCall -> AuthFailure.API
                is VKIDAuthFail.FailedOAuth -> AuthFailure.OAUTH
                is VKIDAuthFail.FailedOAuthState -> AuthFailure.STATE
                is VKIDAuthFail.FailedRedirectActivity -> AuthFailure.REDIRECT
                is VKIDAuthFail.NoBrowserAvailable -> AuthFailure.NO_BROWSER
                else -> AuthFailure.OTHER
            },
            throwable = when (fail) {
                is VKIDAuthFail.FailedApiCall -> fail.throwable
                is VKIDAuthFail.FailedRedirectActivity -> fail.throwable
                is VKIDAuthFail.NoBrowserAvailable -> fail.throwable
                else -> null
            },
            diagnostics = diagnostics)
        if (fail is VKIDAuthFail.Canceled) return
        AuthDiagnostics.record(AuthEvent.CALLBACK_ENQUEUED, trace, expected, session.current)
        scope.launch { operations.withLock {
            AuthDiagnostics.record(AuthEvent.CALLBACK_LOCKED, trace, expected, session.current)
            transition(AuthState.Guest(AuthError.Provider), expected, trace)
        } }
    }

    override suspend fun logout() = operations.withLock {
        val expected = session.current
        // A durable suppression write must succeed before announcing local logout.
        if (!transition(AuthState.Guest(), expected) { policy.setRestoreAllowed(false) }) return@withLock
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
            if (!success) transition(AuthState.Guest(AuthError.LogoutUnconfirmed), loggedOut)
        } catch (_: TimeoutCancellationException) { transition(AuthState.Guest(AuthError.LogoutUnconfirmed), loggedOut) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { transition(AuthState.Guest(AuthError.LogoutUnconfirmed), loggedOut) }
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
        // Widget generation only: OneTap exposes no browser-start callback here.
        val flow = remember(adapter) { AuthDiagnostics.newFlow() }
        DisposableEffect(adapter, flow) {
            AuthDiagnostics.record(AuthEvent.FLOW_READY, flow, expected, adapter.session.current)
            onDispose { AuthDiagnostics.record(AuthEvent.FLOW_DISPOSED, flow, expected, adapter.session.current) }
        }
        OneTap(modifier = Modifier.fillMaxWidth(), scenario = OneTapTitleScenario.SignIn,
            fastAuthEnabled = false, signInAnotherAccountButtonEnabled = true,
            authParams = VKIDAuthUiParams { scopes = emptySet() },
            onAuthCode = { _, completion -> adapter.codeReceived(flow, expected, completion) },
            onAuth = { _, token -> adapter.authenticated(token, expected, flow) },
            onFail = { _, failure -> adapter.failed(failure, expected, flow) })
    }
}
