package com.msafonov.swimresults

import androidx.compose.ui.text.intl.Locale
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.ui.strings.rankFullNameResource
import com.msafonov.swimresults.ui.strings.rankShortNameResource
import com.msafonov.swimresults.ui.strings.styleNameResource
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import swimresults.swimresultsapp.generated.resources.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringsTest {
    @Test
    fun everyStyleAndRankResolvesToItsExistingResource() = runTest {
        val styles = mapOf(
            SwimStyle.FREESTYLE to "Вольный стиль",
            SwimStyle.BACKSTROKE to "На спине",
            SwimStyle.BREASTSTROKE to "Брасс",
            SwimStyle.BUTTERFLY to "Баттерфляй",
            SwimStyle.MEDLEY to "Комплексное плавание"
        )
        assertEquals(SwimStyle.entries.toSet(), styles.keys)
        for ((style, expected) in styles) assertEquals(expected, getString(styleNameResource(style)))
        val ranks = mapOf(
            SwimmingRank.ThirdJunior to ("III юн." to "III юношеский разряд"),
            SwimmingRank.SecondJunior to ("II юн." to "II юношеский разряд"),
            SwimmingRank.FirstJunior to ("I юн." to "I юношеский разряд"),
            SwimmingRank.Third to ("III" to "III спортивный разряд"),
            SwimmingRank.Second to ("II" to "II спортивный разряд"),
            SwimmingRank.First to ("I" to "I спортивный разряд"),
            SwimmingRank.CandidateMasterOfSports to ("КМС" to "Кандидат в мастера спорта"),
            SwimmingRank.MasterOfSports to ("МС" to "Мастер спорта"),
            SwimmingRank.InternationalMasterOfSports to ("МСМК" to "Мастер спорта международного класса")
        )
        assertEquals(SwimmingRank.entries.toSet(), ranks.keys)
        for ((rank, names) in ranks) {
            assertEquals(names.first, getString(rankShortNameResource(rank)))
            assertEquals(names.second, getString(rankFullNameResource(rank)))
        }
    }

    @Test
    fun assignmentPluralsFormatCountsAndNamesWithoutUnresolvedArguments() = runTest {
        val name = "Анна & <Иван> 100%"
        for (count in listOf(1, 2, 5)) {
            val title = getPluralString(Res.plurals.historyLinkTitle, count, count)
            val action = getPluralString(Res.plurals.historyLinkAction, count, count)
            val linked = getPluralString(Res.plurals.historyLinked, count, count, name)
            val limit = getPluralString(Res.plurals.historySelectionLimit, count, count)
            for (text in listOf(title, action, linked, limit)) {
                assertTrue(text.contains(count.toString()), text)
                assertFalse(text.contains("%1\$d"), text)
            }
            assertTrue(linked.endsWith(name), linked)
            assertEquals("$action?", title)
            // Plural categories follow the runtime locale; exercise exact Russian forms
            // when this common test is run in the Russian resource environment.
            if (Locale.current.language == "ru") {
                val noun = when (count) { 1 -> "результат"; 2 -> "результата"; else -> "результатов" }
                assertEquals("Назначить $count $noun", action)
                val verb = if (count == 1) "Назначен" else "Назначено"
                assertEquals("$verb $count $noun: $name", linked)
            }
        }
    }

    @Test
    fun templatesKeepArgumentsLiteralAndUnitsInResources() = runTest {
        assertEquals("Кубок & <финал> 100% (12.06.2026)",
            getString(Res.string.competitionNameDate, "Кубок & <финал> 100%", "12.06.2026"))
        assertEquals("−0.01 с", getString(Res.string.differenceSeconds, "−0.01"))
        assertEquals("100 м На спине", getString(Res.string.distanceStyle, 100, "На спине"))
    }
}
