package com.msafonov.swimresults

import androidx.compose.runtime.Composable
import com.msafonov.swimresults.data.model.RecordedSwim
import com.msafonov.swimresults.data.model.SwimCompetition
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.ui.strings.rankShortNameResource
import com.msafonov.swimresults.ui.strings.styleNameResource
import org.jetbrains.compose.resources.getString
import swimresults.swimresultsapp.generated.resources.*

@Composable
expect fun rememberHistoryShareLauncher(): (String) -> Unit

/** Resolve the complete localized message only after loading the selected record. */
internal suspend fun formatHistoryShareText(competition: SwimCompetition, swim: RecordedSwim): String =
    getString(
        Res.string.historyShareTemplate,
        competition.name,
        shareDate(competition.date),
        shareDate(swim.swimDate),
        swim.distanceMeters,
        getString(styleNameResource(swim.style)),
        getString(if (swim.gender == Swimmer.Men) Res.string.men else Res.string.women),
        swim.poolLengthMeters,
        shareTime(swim.timeCentiseconds),
        swim.qualification?.let { getString(rankShortNameResource(it)) }
            ?: getString(Res.string.historyNoQualification)
    )

private fun shareDate(iso: String): String {
    val parts = iso.split('-')
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else iso
}

private fun shareTime(centiseconds: Long): String {
    val minutes = centiseconds / 6000
    val seconds = (centiseconds / 100 % 60).toString().padStart(2, '0')
    val fraction = (centiseconds % 100).toString().padStart(2, '0')
    return "$minutes:$seconds.$fraction"
}
