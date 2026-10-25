package com.msafonov.swimresults

import com.msafonov.swimresults.data.model.AthleteBirthDate
import kotlinx.datetime.LocalDate
import kotlin.test.*

class AthleteBirthDateTest {
    private val today = LocalDate(2026, 10, 25)

    @Test fun optionalDateAndInclusiveBounds() {
        assertNull(AthleteBirthDate.parse("", today))
        assertNull(AthleteBirthDate.parse("  ", today))
        assertEquals(LocalDate(1900, 1, 1), AthleteBirthDate.parse("01.01.1900", today))
        assertEquals(today, AthleteBirthDate.parse("25.10.2026", today))
    }

    @Test fun strictCalendarRejectsIncompleteImpossibleAndFutureDates() {
        for (value in listOf("31.12.1899", "26.10.2026", "01.01.9999", "29.02.1900", "29.02.2023",
            "31.04.2000", "00.01.2000", "01.00.2000", "01.13.2000", "1.01.2000", "01.1.2000",
            "01.01.00", "01.01.", "2000-01-01", "01/01/2000", "01.01.2000x", " 01.01.2000", "abcdefghij")) {
            assertFalse(AthleteBirthDate.isValid(value, today), value)
            assertFailsWith<IllegalArgumentException>(value) { AthleteBirthDate.parse(value, today) }
        }
    }

    @Test fun leapDayAndRussianIsoRoundTrip() {
        for (value in listOf("29.02.2000", "29.02.2024", "02.03.2012")) {
            val date = AthleteBirthDate.parse(value, today)!!
            assertEquals(value, AthleteBirthDate.format(LocalDate.parse(date.toString())))
        }
        assertEquals("2012-03-02", AthleteBirthDate.parse("02.03.2012", today).toString())
    }
}
