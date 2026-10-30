package com.msafonov.swimresults.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.TimeComponents
import com.msafonov.swimresults.data.model.TimeComponents.Companion.toDuration
import com.msafonov.swimresults.ui.theme.Theme
import com.msafonov.swimresults.ui.theme.SwimRankAppTheme
import com.msafonov.swimresults.viewmodel.ResultComparisonStatus
import com.msafonov.swimresults.viewmodel.ResultDetailsTime
import com.msafonov.swimresults.viewmodel.ResultDetailsUiState
import com.msafonov.swimresults.viewmodel.ResultStandardRow
import org.jetbrains.compose.resources.stringResource
import swimresults.feature.checkresult.impl.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultDialog(details: ResultDetailsUiState, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = Theme.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
        containerColor = Theme.colors.backGround,
        contentColor = Theme.colors.textPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Theme.colors.textSecondary) }
    ) {
        ResultDetailsContent(details)
    }
}

@Composable
private fun ResultDetailsContent(details: ResultDetailsUiState) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
            Column(
                Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    listOf(
                        stringResource(Res.string.distanceMeters, details.distance),
                        styleName(details.swimStyle).replaceFirstChar { it.lowercase() },
                        stringResource(if (details.swimmer == com.msafonov.swimresults.data.model.Swimmer.Women) Res.string.women else Res.string.men)
                            .replaceFirstChar { it.lowercase() },
                        stringResource(if (details.poolType == com.msafonov.swimresults.data.model.PoolType.Short) Res.string.shortWaterLength else Res.string.longWaterLength),
                    ).joinToString(", "),
                    modifier = Modifier.fillMaxWidth(),
                    color = Theme.colors.textSecondary,
                    fontSize = 16.sp,
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.checkedTime, details.input), Modifier.weight(1f),
                        color = Theme.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    details.worldAquaticsPoints?.let { points ->
                        Text(stringResource(Res.string.finaPoints, points), Modifier.weight(1f),
                            color = Theme.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End)
                    }
                }
                val achieved = details.checkedResult?.currentRank
                val qualificationName = when {
                    details.checkedResult == null -> stringResource(Res.string.qualificationUnknown)
                    achieved == null -> stringResource(Res.string.noQualification)
                    else -> rankFullName(achieved)
                }
                Text(
                    qualificationName,
                    color = Theme.colors.primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )

                when (details.comparisonStatus) {
                    ResultComparisonStatus.NoResult -> ComparisonMessage(Res.string.qualificationUnknown)
                    ResultComparisonStatus.StandardsUnavailable -> ComparisonMessage(Res.string.standardsComparisonUnavailable)
                    ResultComparisonStatus.UnsupportedTime -> ComparisonMessage(Res.string.comparisonUnsupportedTime)
                    ResultComparisonStatus.MissingAchievedStandard -> ComparisonMessage(Res.string.achievedStandardMissing)
                    ResultComparisonStatus.Available, ResultComparisonStatus.NoQualification -> {
                        if (details.rows.isEmpty()) ComparisonMessage(Res.string.standardsComparisonUnavailable)
                        else StandardsTable(details, achieved)
                    }
                }
                Text(stringResource(Res.string.comparisonMeaning), color = Theme.colors.textSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StandardsTable(details: ResultDetailsUiState, achieved: SwimmingRank?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TableRow(stringResource(Res.string.tableQualification), stringResource(Res.string.tableStandard), stringResource(Res.string.tableDifference), highlighted = false, description = null)
        details.rows.forEach { row ->
            val highlighted = row.rank == achieved
            val rowDifference = ResultDetailsTime.formatDifference(row.differenceCentiseconds)
            val description = stringResource(
                if (highlighted) Res.string.achievedStandardRowDescription else Res.string.standardRowDescription,
                rankFullName(row.rank), standardTime(row), rowDifference
            )
            val label = if (highlighted) {
                "${rankShortName(row.rank)} · ${stringResource(Res.string.achievedQualification)}"
            } else rankShortName(row.rank)
            TableRow(label, standardTime(row), rowDifference, highlighted, description)
        }
    }
}

@Composable
private fun TableRow(first: String, second: String, third: String, highlighted: Boolean, description: String?) {
    val background = if (highlighted) Theme.colors.primary else Theme.colors.backGround2
    val foreground = if (highlighted) Theme.colors.textContrast else Theme.colors.textPrimary
    Row(Modifier.fillMaxWidth().background(background, Theme.shapes.small).padding(8.dp)
        .then(if (description != null) Modifier.semantics(mergeDescendants = true) { contentDescription = description } else Modifier),
        verticalAlignment = Alignment.CenterVertically) {
        Text(first, Modifier.weight(4f), color = foreground, fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal)
        Text(second, Modifier.weight(3f), color = foreground)
        Text(third, Modifier.weight(3f), color = foreground, fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ComparisonMessage(resource: org.jetbrains.compose.resources.StringResource) {
    Surface(color = Theme.colors.backGround2, shape = Theme.shapes.small) {
        Text(stringResource(resource), Modifier.fillMaxWidth().padding(12.dp), color = Theme.colors.textPrimary)
    }
}

@Composable
private fun standardTime(row: ResultStandardRow): String =
    TimeComponents.durationToComponents(row.time).toRankFormat()

@Composable
private fun styleName(style: com.msafonov.swimresults.data.model.SwimStyle): String = stringResource(when (style) {
    com.msafonov.swimresults.data.model.SwimStyle.FREESTYLE -> Res.string.styleFreestyle
    com.msafonov.swimresults.data.model.SwimStyle.BACKSTROKE -> Res.string.styleBackstroke
    com.msafonov.swimresults.data.model.SwimStyle.BREASTSTROKE -> Res.string.styleBreaststroke
    com.msafonov.swimresults.data.model.SwimStyle.BUTTERFLY -> Res.string.styleButterfly
    com.msafonov.swimresults.data.model.SwimStyle.MEDLEY -> Res.string.styleMedley
})

@Composable
private fun rankFullName(rank: SwimmingRank): String = stringResource(when (rank) {
    SwimmingRank.InternationalMasterOfSports -> Res.string.rankInternationalMaster
    SwimmingRank.MasterOfSports -> Res.string.rankMaster
    SwimmingRank.CandidateMasterOfSports -> Res.string.rankCandidateMaster
    SwimmingRank.First -> Res.string.rankFirst
    SwimmingRank.Second -> Res.string.rankSecond
    SwimmingRank.Third -> Res.string.rankThird
    SwimmingRank.FirstJunior -> Res.string.rankFirstJunior
    SwimmingRank.SecondJunior -> Res.string.rankSecondJunior
    SwimmingRank.ThirdJunior -> Res.string.rankThirdJunior
})

@Composable
private fun rankShortName(rank: SwimmingRank): String = stringResource(when (rank) {
    SwimmingRank.InternationalMasterOfSports -> Res.string.rankShortInternationalMaster
    SwimmingRank.MasterOfSports -> Res.string.rankShortMaster
    SwimmingRank.CandidateMasterOfSports -> Res.string.rankShortCandidateMaster
    SwimmingRank.First -> Res.string.rankShortFirst
    SwimmingRank.Second -> Res.string.rankShortSecond
    SwimmingRank.Third -> Res.string.rankShortThird
    SwimmingRank.FirstJunior -> Res.string.rankShortFirstJunior
    SwimmingRank.SecondJunior -> Res.string.rankShortSecondJunior
    SwimmingRank.ThirdJunior -> Res.string.rankShortThirdJunior
})

@Preview(name = "Details light", widthDp = 360, heightDp = 800)
@Preview(name = "Details narrow large text", widthDp = 320, heightDp = 640, fontScale = 2f)
@Composable
private fun ResultDetailsPreview() {
    SwimRankAppTheme {
        val rank = SwimmingRank.Second
        val time = TimeComponents(1, 20, 0).toDuration()
        val details = ResultDetailsUiState(100, com.msafonov.swimresults.data.model.SwimStyle.FREESTYLE,
            com.msafonov.swimresults.data.model.Swimmer.Women, com.msafonov.swimresults.data.model.PoolType.Short,
            "1:22.00", com.msafonov.swimresults.data.model.CheckedResult(rank, null, null, null),
            ResultComparisonStatus.Available, listOf(ResultStandardRow(rank, time, 200)), worldAquaticsPoints = 500)
        ResultDetailsContent(details)
    }
}
