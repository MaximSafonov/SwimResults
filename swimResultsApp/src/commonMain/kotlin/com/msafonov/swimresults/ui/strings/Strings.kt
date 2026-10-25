package com.msafonov.swimresults.ui.strings

import androidx.compose.runtime.Composable
import com.msafonov.swimresults.data.model.AthleteBirthDate
import kotlinx.datetime.LocalDate
import com.msafonov.swimresults.data.model.HistoryLabel
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.viewmodel.AthleteFormError
import com.msafonov.swimresults.viewmodel.HistoryMessage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import swimresults.swimresultsapp.generated.resources.*

@Composable
internal fun styleName(style: SwimStyle): String = stringResource(styleNameResource(style))

internal fun styleNameResource(style: SwimStyle): StringResource = when (style) {
    SwimStyle.FREESTYLE -> Res.string.styleFreestyle
    SwimStyle.BACKSTROKE -> Res.string.styleBackstroke
    SwimStyle.BREASTSTROKE -> Res.string.styleBreaststroke
    SwimStyle.BUTTERFLY -> Res.string.styleButterfly
    SwimStyle.MEDLEY -> Res.string.styleMedley
}

@Composable
internal fun rankFullName(rank: SwimmingRank): String = stringResource(rankFullNameResource(rank))

internal fun rankFullNameResource(rank: SwimmingRank): StringResource = when (rank) {
    SwimmingRank.InternationalMasterOfSports -> Res.string.rankInternationalMaster
    SwimmingRank.MasterOfSports -> Res.string.rankMaster
    SwimmingRank.CandidateMasterOfSports -> Res.string.rankCandidateMaster
    SwimmingRank.First -> Res.string.rankFirst
    SwimmingRank.Second -> Res.string.rankSecond
    SwimmingRank.Third -> Res.string.rankThird
    SwimmingRank.FirstJunior -> Res.string.rankFirstJunior
    SwimmingRank.SecondJunior -> Res.string.rankSecondJunior
    SwimmingRank.ThirdJunior -> Res.string.rankThirdJunior
}

@Composable
internal fun rankShortName(rank: SwimmingRank): String = stringResource(rankShortNameResource(rank))

internal fun rankShortNameResource(rank: SwimmingRank): StringResource = when (rank) {
    SwimmingRank.InternationalMasterOfSports -> Res.string.rankShortInternationalMaster
    SwimmingRank.MasterOfSports -> Res.string.rankShortMaster
    SwimmingRank.CandidateMasterOfSports -> Res.string.rankShortCandidateMaster
    SwimmingRank.First -> Res.string.rankShortFirst
    SwimmingRank.Second -> Res.string.rankShortSecond
    SwimmingRank.Third -> Res.string.rankShortThird
    SwimmingRank.FirstJunior -> Res.string.rankShortFirstJunior
    SwimmingRank.SecondJunior -> Res.string.rankShortSecondJunior
    SwimmingRank.ThirdJunior -> Res.string.rankShortThirdJunior
}

@Composable
internal fun historyLabel(label: HistoryLabel): String = when (label) {
    HistoryLabel.DeviceLocal -> stringResource(Res.string.historyDeviceLocal)
    is HistoryLabel.AthleteName -> label.name
    HistoryLabel.SelectAthlete -> stringResource(Res.string.historySelectAthlete)
    HistoryLabel.LoadError -> stringResource(Res.string.historyLoadError)
}

@Composable
internal fun athleteFormError(error: AthleteFormError): String = stringResource(when (error) {
    AthleteFormError.SessionChanged -> Res.string.athleteSessionChanged
    AthleteFormError.Unavailable -> Res.string.athleteUnavailable
    AthleteFormError.LoadFailed -> Res.string.athleteLoadFailed
    AthleteFormError.InvalidInput -> Res.string.athleteInvalidInput
    AthleteFormError.SaveFailed -> Res.string.athleteSaveFailed
})

@Composable
internal fun historyMessage(message: HistoryMessage): String = when (message) {
    HistoryMessage.AthleteChanged -> stringResource(Res.string.historyAthleteChanged)
    HistoryMessage.RecordsChanged -> stringResource(Res.string.historyRecordsChanged)
    HistoryMessage.LinkFailed -> stringResource(Res.string.historyLinkFailed)
    is HistoryMessage.SelectionLimit -> pluralStringResource(Res.plurals.historySelectionLimit, message.limit, message.limit)
    is HistoryMessage.Linked -> pluralStringResource(Res.plurals.historyLinked, message.count, message.count, message.athleteName)
}

@Composable
internal fun athleteMetadata(birthDate: LocalDate?, rank: SwimmingRank?, gender: Swimmer?): String {
    val date = birthDate?.let(AthleteBirthDate::format)
    val rankName = rank?.let { rankShortName(it) }
    val genderName = gender?.let { stringResource(if (it == Swimmer.Men) Res.string.sportsGenderMen else Res.string.sportsGenderWomen) }
    val parts = listOfNotNull(date, rankName, genderName)
    return when (parts.size) {
        3 -> stringResource(Res.string.athleteMetadataFull, parts[0], parts[1], parts[2])
        2 -> stringResource(Res.string.athleteMetadata, parts[0], parts[1])
        else -> parts.firstOrNull().orEmpty()
    }
}
