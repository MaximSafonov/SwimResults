package com.msafonov.swimresults

import androidx.compose.runtime.Composable
import com.msafonov.swimresults.data.model.RecordedSwim
import com.msafonov.swimresults.data.model.SwimCompetition
import com.msafonov.swimresults.data.model.Swimmer
import org.jetbrains.compose.resources.stringResource
import swimresults.feature.history.impl.generated.resources.Res
import swimresults.feature.history.impl.generated.resources.competitionLabel
import swimresults.feature.history.impl.generated.resources.genderGroup
import swimresults.feature.history.impl.generated.resources.historyCompetitionDate
import swimresults.feature.history.impl.generated.resources.historyDate
import swimresults.feature.history.impl.generated.resources.historyDistance
import swimresults.feature.history.impl.generated.resources.historyMeters
import swimresults.feature.history.impl.generated.resources.historyNoQualification
import swimresults.feature.history.impl.generated.resources.historyPool
import swimresults.feature.history.impl.generated.resources.historyQualification
import swimresults.feature.history.impl.generated.resources.historyTime
import swimresults.feature.history.impl.generated.resources.men
import swimresults.feature.history.impl.generated.resources.women

@Composable
expect fun rememberHistoryShareLauncher(): (String) -> Unit

internal data class HistoryShareLabels(
    val competition: String,
    val competitionDate: String,
    val swimDate: String,
    val distance: String,
    val gender: String,
    val pool: String,
    val time: String,
    val qualification: String,
    val meters: String,
    val men: String,
    val women: String,
    val noQualification: String
)

internal fun formatHistoryShareText(
    competition: SwimCompetition,
    swim: RecordedSwim,
    labels: HistoryShareLabels
): String {
    val minutes = swim.timeCentiseconds / 6000
    val seconds = (swim.timeCentiseconds / 100 % 60).toString().padStart(2, '0')
    val fraction = (swim.timeCentiseconds % 100).toString().padStart(2, '0')
    fun date(iso: String): String {
        val parts = iso.split('-')
        return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else iso
    }
    return listOf(
        "${labels.competition}: ${competition.name}",
        "${labels.competitionDate}: ${date(competition.date)}",
        "${labels.swimDate}: ${date(swim.swimDate)}",
        "${labels.distance}: ${swim.distanceMeters} ${labels.meters}, ${swim.style.text}",
        "${labels.gender}: ${if (swim.gender == Swimmer.Men) labels.men else labels.women}",
        "${labels.pool}: ${swim.poolLengthMeters} ${labels.meters}",
        "${labels.time}: $minutes:$seconds.$fraction",
        "${labels.qualification}: ${swim.qualification?.text ?: labels.noQualification}"
    ).joinToString("\n")
}

@Composable
internal fun historyShareText(competition: SwimCompetition, swim: RecordedSwim): String =
    formatHistoryShareText(competition, swim, HistoryShareLabels(
        competition = stringResource(Res.string.competitionLabel),
        competitionDate = stringResource(Res.string.historyCompetitionDate),
        swimDate = stringResource(Res.string.historyDate),
        distance = stringResource(Res.string.historyDistance),
        gender = stringResource(Res.string.genderGroup),
        pool = stringResource(Res.string.historyPool),
        time = stringResource(Res.string.historyTime),
        qualification = stringResource(Res.string.historyQualification),
        meters = stringResource(Res.string.historyMeters),
        men = stringResource(Res.string.men),
        women = stringResource(Res.string.women),
        noQualification = stringResource(Res.string.historyNoQualification)
    ))
