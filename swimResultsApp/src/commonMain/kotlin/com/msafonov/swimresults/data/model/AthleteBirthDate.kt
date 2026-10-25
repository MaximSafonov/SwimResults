package com.msafonov.swimresults.data.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** The optional athlete date uses a strict Russian input format and ISO storage. */
object AthleteBirthDate {
    private val inputPattern = Regex("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}")
    private val earliest = LocalDate(1900, 1, 1)

    fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    fun parse(value: String, today: LocalDate = today()): LocalDate? {
        if (value.isBlank()) return null
        require(inputPattern.matches(value)) { "Invalid birth date format" }
        val date = LocalDate(value.substring(6).toInt(), value.substring(3, 5).toInt(), value.substring(0, 2).toInt())
        require(date in earliest..today) { "Invalid birth date range" }
        return date
    }

    fun isValid(value: String, today: LocalDate = today()): Boolean =
        try { parse(value, today); true } catch (_: IllegalArgumentException) { false }

    fun format(date: LocalDate): String =
        "${date.day.toString().padStart(2, '0')}.${date.month.number.toString().padStart(2, '0')}.${date.year.toString().padStart(4, '0')}"
}
