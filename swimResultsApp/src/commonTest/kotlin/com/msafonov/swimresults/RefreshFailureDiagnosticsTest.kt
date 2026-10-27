package com.msafonov.swimresults

import com.msafonov.swimresults.auth.RefreshApiError
import com.msafonov.swimresults.auth.RefreshFailureDiagnostics
import com.msafonov.swimresults.auth.RefreshStage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RefreshFailureDiagnosticsTest {
    @Test
    fun recognizesOnlyExactStagePrefixes() {
        assertEquals(RefreshStage.REFRESH_TOKEN, parse("Failed code to refresh token due to: details").stage)
        assertEquals(RefreshStage.USER_INFO, parse("Failed to fetch user data due to: details").stage)
        assertEquals(RefreshStage.UNKNOWN, parse("prefix Failed code to refresh token due to: details").stage)
        assertEquals(RefreshStage.UNKNOWN, parse(null).stage)
    }

    @Test
    fun recognizesOnlyExactInteractiveStagePrefixes() {
        assertEquals(RefreshStage.CODE_TO_TOKEN,
            interactive("Failed code to token exchange api call: details").stage)
        assertEquals(RefreshStage.USER_INFO, interactive("Failed to fetch user data").stage)
        assertEquals(RefreshStage.UNKNOWN,
            interactive("prefix Failed code to token exchange api call: details").stage)
        assertEquals(RefreshStage.UNKNOWN,
            interactive("Failed code to token exchange api call").stage)
        assertEquals(RefreshStage.UNKNOWN,
            interactive("Failed to fetch user data due to: details").stage)
        assertEquals(RefreshStage.UNKNOWN, interactive(null).stage)
    }

    @Test
    fun interactiveTokenExchangeParsesOnlySafeStatusAndAllowlistedApiError() {
        val result = interactive(
            "Failed code to token exchange api call: details",
            "Api error: 401 {\"error\":\"invalid_grant\",\"description\":\"private-marker\"}",
        )
        assertEquals(401, result.httpStatus)
        assertEquals(RefreshApiError.INVALID_GRANT, result.apiError)
        assertEquals("RefreshFailureDiagnostics(stage=CODE_TO_TOKEN, httpStatus=401, apiError=INVALID_GRANT)", result.toString())
    }

    @Test
    fun interactiveUserInfoAndUnknownNeverInferStatusOrApiError() {
        val message = "Api error: 401 {\"error\":\"invalid_token\"}"
        val userInfo = interactive("Failed to fetch user data", message)
        assertNull(userInfo.httpStatus)
        assertEquals(RefreshApiError.UNAVAILABLE, userInfo.apiError)
        val unknown = interactive("unknown", message)
        assertNull(unknown.httpStatus)
        assertEquals(RefreshApiError.UNAVAILABLE, unknown.apiError)
        val malformed = interactive("Failed code to token exchange api call: x", "private-marker")
        assertNull(malformed.httpStatus)
        assertEquals(RefreshApiError.UNAVAILABLE, malformed.apiError)
        assertEquals("RefreshFailureDiagnostics(stage=CODE_TO_TOKEN, httpStatus=null, apiError=UNAVAILABLE)", malformed.toString())
    }

    @Test
    fun parsesHttpStatusAndAllowlistedApiError() {
        val result = parse(message = "Api error: 401 {\"error\":\"invalid_grant\"}")
        assertEquals(401, result.httpStatus)
        assertEquals(RefreshApiError.INVALID_GRANT, result.apiError)
        assertEquals(503, parse(message = "Api error: 503 {\"error\":\"server_error\"}").httpStatus)
    }

    @Test
    fun preservesStatusWhenBodyCannotBeParsedAndNeverReturnsRawInput() {
        val secretBody = "{\"error\":\"private-secret\",\"access_token\":\"token-marker\"}"
        val result = parse(message = "Api error: 400 $secretBody")
        assertEquals(400, result.httpStatus)
        assertEquals(RefreshApiError.OTHER, result.apiError)
        assertEquals("RefreshFailureDiagnostics(stage=REFRESH_TOKEN, httpStatus=400, apiError=OTHER)", result.toString())
    }

    @Test
    fun userInfoAndUnknownStagesNeverInferStatusOrApiError() {
        val userInfo = parse("Failed to fetch user data due to: x", "Api error: 401 {\"error\":\"invalid_token\"}")
        assertNull(userInfo.httpStatus)
        assertEquals(RefreshApiError.UNAVAILABLE, userInfo.apiError)
        val unknown = parse("unexpected", "Api error: 401 {\"error\":\"invalid_token\"}")
        assertNull(unknown.httpStatus)
        assertEquals(RefreshApiError.UNAVAILABLE, unknown.apiError)
    }

    @Test
    fun rejectsMalformedOversizedDeepAndDuplicateErrorJson() {
        assertEquals(RefreshApiError.UNAVAILABLE, api("{bad"))
        assertEquals(RefreshApiError.UNAVAILABLE, api("{\"error\":\"invalid_grant\",\"error\":\"server_error\"}"))
        assertEquals(RefreshApiError.UNAVAILABLE, api("{\"error\":\"invalid_grant\",\"nested\":{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":{\"f\":{\"g\":{\"h\":0}}}}}}}}}"))
        assertEquals(RefreshApiError.UNAVAILABLE, api("{\"error\":\"invalid_grant\"}" + " ".repeat(4096)))
    }

    @Test
    fun acceptsOnlyStrictStatusEnvelope() {
        listOf("Api error: 099 {}", "Api error: 600 {}", "Api error: 401x{}", "text Api error: 401 {}", "Api error: 40 {}")
            .forEach { assertNull(parse(message = it).httpStatus) }
    }

    private fun interactive(description: String?, message: String? = "Api error: 400 {\"error\":\"invalid_request\"}") =
        RefreshFailureDiagnostics.parseInteractive(description, message?.let(::IllegalStateException))

    private fun interactiveResult(throwable: Throwable) = RefreshFailureDiagnostics.parseInteractive(
        "Failed code to token exchange api call: details", throwable,
    )

    @Test
    fun interactiveTokenExchangeFindsNestedCauseAndNearestValidEnvelopeWins() {
        val outer = IllegalStateException("wrapper", IllegalArgumentException("Api error: 403 {\"error\":\"access_denied\"}"))
        val nested = RefreshFailureDiagnostics.parseInteractive(
            "Failed code to token exchange api call: details", outer,
        )
        assertEquals(403, nested.httpStatus)
        assertEquals(RefreshApiError.ACCESS_DENIED, nested.apiError)

        val nearer = IllegalStateException(
            "Api error: 401 {\"error\":\"invalid_grant\"}",
            IllegalArgumentException("Api error: 500 {\"error\":\"server_error\"}"),
        )
        val nearest = RefreshFailureDiagnostics.parseInteractive(
            "Failed code to token exchange api call: details", nearer,
        )
        assertEquals(401, nearest.httpStatus)
        assertEquals(RefreshApiError.INVALID_GRANT, nearest.apiError)
    }

    @Test
    fun malformedAndOversizedMessagesAreSkippedButValidEnvelopeStopsTraversal() {
        val malformedThenValid = IllegalStateException("Api error: 40 {}", IllegalArgumentException("Api error: 404 {}"))
        assertEquals(404, interactiveResult(malformedThenValid).httpStatus)

        val tooLong = "Api error: 400 " + "x".repeat(4097)
        val oversizedThenValid = IllegalStateException(tooLong, IllegalArgumentException("Api error: 403 {}"))
        assertEquals(403, interactiveResult(oversizedThenValid).httpStatus)

        val malformedBodyThenValid = IllegalStateException(
            "Api error: 401 {bad}", IllegalArgumentException("Api error: 500 {\"error\":\"server_error\"}"),
        )
        val result = interactiveResult(malformedBodyThenValid)
        assertEquals(401, result.httpStatus)
        assertEquals(RefreshApiError.UNAVAILABLE, result.apiError)
    }

    @Test
    fun interactiveCauseTraversalIsLimitedToFourNodesAndStopsOnCycles() {
        val fifth = IllegalArgumentException("Api error: 409 {}")
        val fourth = IllegalStateException("wrapper", fifth)
        val third = IllegalStateException("wrapper", fourth)
        val second = IllegalStateException("wrapper", third)
        val first = IllegalStateException("wrapper", second)
        assertNull(interactiveResult(first).httpStatus)

        val a = IllegalStateException("wrapper")
        val b = IllegalStateException("Api error: 410 {}")
        a.initCause(b)
        b.initCause(a)
        assertEquals(410, interactiveResult(a).httpStatus)

        val cyclicNoMatchA = IllegalStateException("wrapper")
        val cyclicNoMatchB = IllegalStateException("wrapper")
        cyclicNoMatchA.initCause(cyclicNoMatchB)
        cyclicNoMatchB.initCause(cyclicNoMatchA)
        assertNull(interactiveResult(cyclicNoMatchA).httpStatus)
    }

    private fun api(body: String) = parse(message = "Api error: 400 $body").apiError

    private fun parse(
        description: String? = "Failed code to refresh token due to: failure",
        message: String? = "Api error: 400 {\"error\":\"invalid_request\"}",
    ) = RefreshFailureDiagnostics.parse(description, message)
}
