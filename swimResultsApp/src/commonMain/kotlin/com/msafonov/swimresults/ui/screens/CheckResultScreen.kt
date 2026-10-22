package com.msafonov.swimresults.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.ui.composable.QualificationGrid
import com.msafonov.swimresults.ui.composable.SwimFilterChip
import com.msafonov.swimresults.ui.composable.TimerInput
import com.msafonov.swimresults.ui.dialogs.ResultDialog
import com.msafonov.swimresults.ui.theme.Theme
import com.msafonov.swimresults.ui.theme.SwimComponentDefaults
import com.msafonov.swimresults.ui.theme.SwimRankAppTheme
import com.msafonov.swimresults.viewmodel.CheckResultUiState
import com.msafonov.swimresults.viewmodel.withTable
import com.msafonov.swimresults.data.storage.RankTableStorage
import com.msafonov.swimresults.data.model.RankTableType
import com.msafonov.swimresults.viewmodel.CheckResultViewModel
import com.msafonov.swimresults.viewmodel.StandardsStatus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import swimresults.swimresultsapp.generated.resources.*

@Composable
fun CheckResultScreen(passedResult: String? = null) {
    val viewModel = koinViewModel<CheckResultViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.startStopwatchUpdates()
        onPauseOrDispose { viewModel.stopStopwatchUpdates() }
    }
    CheckResultContent(
        state = state,
        onSwimmer = viewModel::setSwimmer,
        onPool = viewModel::checkPool,
        onStyle = viewModel::setSwimStyle,
        onDistance = viewModel::setDistance,
        onRetry = viewModel::retry,
        onToggleResult = viewModel::toggleResultExpanded,
        onInput = viewModel::onInputChanged,
        onCheck = viewModel::checkResult,
        onDismissResult = viewModel::dismissResult
    )
}

@Composable
internal fun CheckResultContent(
    state: CheckResultUiState,
    onSwimmer: (Swimmer) -> Unit,
    onPool: (PoolType) -> Unit,
    onStyle: (SwimStyle) -> Unit,
    onDistance: (Int) -> Unit,
    onRetry: () -> Unit,
    onToggleResult: () -> Unit,
    onInput: (TextFieldValue) -> Unit,
    onCheck: () -> Unit,
    onDismissResult: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier.fillMaxSize().background(Theme.colors.backGround).imePadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterGroup(stringResource(Res.string.genderGroup), modifier = Modifier.weight(1f)) {
                SwimFilterChip(selected = state.filters.swimmer == Swimmer.Men,
                    text = stringResource(Res.string.men), onClick = { onSwimmer(Swimmer.Men) })
                SwimFilterChip(selected = state.filters.swimmer == Swimmer.Women,
                    text = stringResource(Res.string.women), onClick = { onSwimmer(Swimmer.Women) })
            }
            FilterGroup(stringResource(Res.string.poolGroup), modifier = Modifier.weight(1f)) {
                SwimFilterChip(selected = state.filters.poolType == PoolType.Short,
                    text = stringResource(Res.string.shortWaterLength), onClick = { onPool(PoolType.Short) })
                SwimFilterChip(selected = state.filters.poolType == PoolType.Long,
                    text = stringResource(Res.string.longWaterLength), onClick = { onPool(PoolType.Long) })
            }
        }
        FilterGroup(stringResource(Res.string.styleGroup)) {
            listOf(SwimStyle.FREESTYLE, SwimStyle.BACKSTROKE, SwimStyle.BREASTSTROKE,
                SwimStyle.BUTTERFLY, SwimStyle.MEDLEY).forEach { style ->
                SwimFilterChip(selected = state.filters.swimStyle == style,
                    text = style.text, onClick = { onStyle(style) })
            }
        }
        FilterGroup(stringResource(Res.string.distanceTitle)) {
            state.filters.distances.forEach { distance ->
                SwimFilterChip(selected = state.filters.distance == distance,
                    text = stringResource(Res.string.distanceMeters, distance),
                    onClick = { onDistance(distance) })
            }
        }
        Text(
            stringResource(Res.string.standardsTitle), color = Theme.colors.textPrimary,
            fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() }
        )
        when (state.status) {
            StandardsStatus.Loading -> {
                CircularProgressIndicator(color = Theme.colors.textPrimary)
                StatusMessage(stringResource(Res.string.standardsLoading))
            }
            StandardsStatus.Error -> {
                StatusMessage(stringResource(Res.string.standardsError))
                TextButton(onClick = onRetry, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
                    Text(stringResource(Res.string.retryStandards), color = Theme.colors.textPrimary)
                }
            }
            StandardsStatus.Unavailable -> {
                StatusMessage(stringResource(Res.string.standardsUnavailable))
                QualificationGrid(state.standards)
            }
            StandardsStatus.Ready -> QualificationGrid(state.standards)
        }
        val expanded = stringResource(if (state.resultExpanded) Res.string.sectionExpanded else Res.string.sectionCollapsed)
        TextButton(
            onClick = onToggleResult,
            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
                .semantics { stateDescription = expanded }
        ) {
            Text(stringResource(Res.string.checkOwnResult), color = Theme.colors.textPrimary)
        }
        if (state.resultExpanded) {
            TimerInput(value = state.timerInput, onValueChange = onInput)
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onCheck()
                },
                enabled = state.canCheckResult,
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                colors = SwimComponentDefaults.primaryButtonColors()
            ) {
                Text(stringResource(Res.string.checkResult))
            }
        }
    }
    state.resultDetails?.let { ResultDialog(details = it, onDismiss = onDismissResult) }
}

@Composable
private fun FilterGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Theme.colors.textPrimary, modifier = Modifier.semantics { heading() })
        FlowRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) { content() }
    }
}

@Composable
private fun StatusMessage(message: String) {
    Text(message, color = Theme.colors.textPrimary,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Preview(name = "Compact", widthDp = 360, heightDp = 800)
@Preview(name = "Narrow", widthDp = 320, heightDp = 640)
@Preview(name = "Tablet", widthDp = 840, heightDp = 800)
@Preview(name = "Large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun StandardsPreview() {
    StandardsThemePreview(isDark = false)
}

@Preview(name = "Dark", widthDp = 360, heightDp = 800)
@Composable
private fun StandardsDarkPreview() {
    StandardsThemePreview(isDark = true)
}

@Composable
private fun StandardsThemePreview(isDark: Boolean) {
    SwimRankAppTheme(isDark = isDark) {
        val state = androidx.compose.runtime.remember {
            CheckResultUiState().withTable(RankTableStorage().rankTable(RankTableType.ShortWaterWomen))
        }
        CheckResultContent(
            state = state, onSwimmer = {}, onPool = {}, onStyle = {}, onDistance = {},
            onRetry = {}, onToggleResult = {}, onInput = {}, onCheck = {}, onDismissResult = {}
        )
    }
}
