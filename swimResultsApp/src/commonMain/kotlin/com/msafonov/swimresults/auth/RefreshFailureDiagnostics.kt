package com.msafonov.swimresults.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal enum class RefreshStage { REFRESH_TOKEN, CODE_TO_TOKEN, USER_INFO, UNKNOWN }

internal enum class RefreshApiError {
    INVALID_REQUEST,
    INVALID_CLIENT,
    INVALID_GRANT,
    UNAUTHORIZED_CLIENT,
    UNSUPPORTED_GRANT_TYPE,
    INVALID_SCOPE,
    INVALID_TOKEN,
    ACCESS_DENIED,
    SERVER_ERROR,
    TEMPORARILY_UNAVAILABLE,
    OTHER,
    UNAVAILABLE,
}

internal data class RefreshFailureDiagnostics(
    val stage: RefreshStage,
    val httpStatus: Int?,
    val apiError: RefreshApiError,
) {
    companion object {
        private const val USER_INFO_PREFIX = "Failed to fetch user data due to: "
        private const val REFRESH_TOKEN_PREFIX = "Failed code to refresh token due to: "
        private const val CODE_TO_TOKEN_PREFIX = "Failed code to token exchange api call: "
        private const val INTERACTIVE_USER_INFO_DESCRIPTION = "Failed to fetch user data"
        private const val API_ERROR_PREFIX = "Api error: "
        private const val MAX_BODY_LENGTH = 4096
        private const val MAX_API_MESSAGE_LENGTH = API_ERROR_PREFIX.length + 4 + MAX_BODY_LENGTH
        private const val MAX_CAUSE_DEPTH = 4
        private const val MAX_JSON_DEPTH = 8

        private val json = Json

        fun parse(description: String?, message: String?): RefreshFailureDiagnostics {
            val stage = when {
                description?.startsWith(REFRESH_TOKEN_PREFIX) == true -> RefreshStage.REFRESH_TOKEN
                description?.startsWith(USER_INFO_PREFIX) == true -> RefreshStage.USER_INFO
                else -> RefreshStage.UNKNOWN
            }
            if (stage != RefreshStage.REFRESH_TOKEN) {
                return RefreshFailureDiagnostics(stage, null, RefreshApiError.UNAVAILABLE)
            }

            return parseApiMessage(stage, message)
                ?: RefreshFailureDiagnostics(stage, null, RefreshApiError.UNAVAILABLE)
        }

        fun parseInteractive(description: String?, throwable: Throwable?): RefreshFailureDiagnostics {
            val stage = when {
                description?.startsWith(CODE_TO_TOKEN_PREFIX) == true -> RefreshStage.CODE_TO_TOKEN
                description == INTERACTIVE_USER_INFO_DESCRIPTION -> RefreshStage.USER_INFO
                else -> RefreshStage.UNKNOWN
            }
            if (stage != RefreshStage.CODE_TO_TOKEN) {
                return RefreshFailureDiagnostics(stage, null, RefreshApiError.UNAVAILABLE)
            }
            var current = throwable
            val visited = mutableListOf<Throwable>()
            var depth = 0
            while (current != null && depth < MAX_CAUSE_DEPTH && visited.none { it === current }) {
                val failure = current
                visited += failure
                val message = failure.message
                if (message != null && message.length <= MAX_API_MESSAGE_LENGTH) {
                    parseApiMessage(stage, message)?.let { return it }
                }
                current = failure.cause
                depth++
            }
            return RefreshFailureDiagnostics(stage, null, RefreshApiError.UNAVAILABLE)
        }

        private fun parseApiMessage(stage: RefreshStage, message: String?): RefreshFailureDiagnostics? {
            val apiMessage = message?.takeIf { it.startsWith(API_ERROR_PREFIX) } ?: return null
            val statusStart = API_ERROR_PREFIX.length
            if (apiMessage.length < statusStart + 4) {
                return null
            }
            val digits = apiMessage.substring(statusStart, statusStart + 3)
            if (digits.any { it !in '0'..'9' } || apiMessage[statusStart + 3] != ' ') {
                return null
            }
            val status = digits.toIntOrNull()?.takeIf { it in 100..599 }
                ?: return null
            val bodyStart = statusStart + 4
            val bodyLength = apiMessage.length - bodyStart
            val apiError = if (bodyLength <= MAX_BODY_LENGTH) {
                parseApiError(apiMessage.substring(bodyStart))
            } else {
                RefreshApiError.UNAVAILABLE
            }
            return RefreshFailureDiagnostics(stage, status, apiError)
        }

        private fun parseApiError(body: String): RefreshApiError {
            if (body.length > MAX_BODY_LENGTH || !hasSafeJsonShape(body)) return RefreshApiError.UNAVAILABLE
            return try {
                val error = (json.parseToJsonElement(body) as? JsonObject)
                    ?.get("error") as? JsonPrimitive
                if (error == null || !error.isString) return RefreshApiError.UNAVAILABLE
                when (error.content) {
                    "invalid_request" -> RefreshApiError.INVALID_REQUEST
                    "invalid_client" -> RefreshApiError.INVALID_CLIENT
                    "invalid_grant" -> RefreshApiError.INVALID_GRANT
                    "unauthorized_client" -> RefreshApiError.UNAUTHORIZED_CLIENT
                    "unsupported_grant_type" -> RefreshApiError.UNSUPPORTED_GRANT_TYPE
                    "invalid_scope" -> RefreshApiError.INVALID_SCOPE
                    "invalid_token" -> RefreshApiError.INVALID_TOKEN
                    "access_denied" -> RefreshApiError.ACCESS_DENIED
                    "server_error" -> RefreshApiError.SERVER_ERROR
                    "temporarily_unavailable" -> RefreshApiError.TEMPORARILY_UNAVAILABLE
                    else -> RefreshApiError.OTHER
                }
            } catch (_: Throwable) {
                RefreshApiError.UNAVAILABLE
            }
        }

        // Bound nesting and reject duplicate top-level `error` keys before JsonObject can collapse them.
        private fun hasSafeJsonShape(value: String): Boolean {
            var depth = 0
            var index = 0
            var rootErrorKeys = 0
            while (index < value.length) {
                when (val char = value[index]) {
                    '"' -> {
                        val stringStart = index
                        index++
                        var escaped = false
                        var closed = false
                        while (index < value.length) {
                            val current = value[index++]
                            if (escaped) escaped = false
                            else if (current == '\\') escaped = true
                            else if (current == '"') {
                                closed = true
                                break
                            }
                        }
                        if (!closed) return false
                        if (depth == 1 && decodeJsonString(value.substring(stringStart, index)) == "error") {
                            var lookahead = index
                            while (value.getOrNull(lookahead)?.isWhitespace() == true) lookahead++
                            if (value.getOrNull(lookahead) == ':') {
                                rootErrorKeys++
                                if (rootErrorKeys > 1) return false
                            }
                        }
                    }
                    '{', '[' -> {
                        depth++
                        if (depth > MAX_JSON_DEPTH) return false
                        index++
                    }
                    '}', ']' -> {
                        depth--
                        if (depth < 0) return false
                        index++
                    }
                    else -> index++
                }
            }
            return depth == 0
        }

        private fun decodeJsonString(token: String): String? = try {
            (json.parseToJsonElement(token) as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.content
        } catch (_: Throwable) {
            null
        }
    }
}
