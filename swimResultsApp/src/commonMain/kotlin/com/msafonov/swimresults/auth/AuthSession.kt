package com.msafonov.swimresults.auth

/** Only the provider's confirmed identity crosses the platform boundary. Never tokens. */
data class AccountPrincipal(val userId: String, val displayName: String) {
    val accountId: String get() = "vk:$userId"
    init { require(userId.isNotBlank()) }
}

enum class AuthError { Storage, Network, Provider, Expired, LogoutUnconfirmed }

sealed interface AuthState {
    data object NotConfigured : AuthState
    data object Unsupported : AuthState
    data object Restoring : AuthState
    data class Guest(val error: AuthError? = null) : AuthState
    data class Authenticated(val principal: AccountPrincipal) : AuthState
    data class ReauthenticationRequired(val error: AuthError) : AuthState
}

interface AuthSession {
    suspend fun restore()
    suspend fun logout()
}
