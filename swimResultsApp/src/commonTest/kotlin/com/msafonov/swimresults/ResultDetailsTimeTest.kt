package com.msafonov.swimresults

import com.msafonov.swimresults.viewmodel.ResultDetailsTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds

class ResultDetailsTimeTest {
    @Test
    fun parsesSecondsAndMinutesAsIntegerCentiseconds() {
        // Matches CheckResultRepository: missing ':' keeps the input for seconds,
        // while "29.00" is not a Long minute value and therefore falls back to 0.
        assertEquals(2_900L, ResultDetailsTime.inputCentiseconds("29.00"))
        assertEquals(2_900L, ResultDetailsTime.inputCentiseconds("0:29.00"))
        assertEquals(9_000L, ResultDetailsTime.inputCentiseconds("1:30.00"))
        assertEquals(5_999L, ResultDetailsTime.inputCentiseconds("59.99"))
        assertEquals(0L, ResultDetailsTime.inputCentiseconds("00.00"))
    }

    @Test
    fun rejectsUnsupportedAndOverflowingFormats() {
        listOf("1", "12", "29", "1:60.00", "1:5.00", "1:00.123", "1:00", "999999999999999999999:00.00")
            .forEach { assertNull(ResultDetailsTime.inputCentiseconds(it), it) }
    }

    @Test
    fun formatsSignedDifferencesAcrossMinuteBoundaries() {
        assertEquals(450L, ResultDetailsTime.difference(9_000, 8_550))
        assertEquals(-1L, ResultDetailsTime.difference(5_998, 5_999))
        assertEquals(0L, ResultDetailsTime.difference(6_000, 6_000))
        assertEquals(6_001L, ResultDetailsTime.difference(12_000, 5_999))
        assertEquals("+4.50", ResultDetailsTime.formatDifference(450))
        assertEquals("−0.01", ResultDetailsTime.formatDifference(-1))
        assertEquals("0.00", ResultDetailsTime.formatDifference(0))
        assertEquals("+60.01", ResultDetailsTime.formatDifference(6_001))
    }

    @Test
    fun decodesLegacyHundredthsAndRejectsInvalidEncoding() {
        assertEquals(8_550L, ResultDetailsTime.standardCentiseconds(85_050L.milliseconds))
        assertEquals(5_999L, ResultDetailsTime.standardCentiseconds(59_099L.milliseconds))
        assertNull(ResultDetailsTime.standardCentiseconds(59_100L.milliseconds))
        assertNull(ResultDetailsTime.standardCentiseconds((-1L).milliseconds))
    }
}
