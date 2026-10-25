package com.msafonov.swimresults.ui.screens

import com.msafonov.swimresults.ui.strings.*
import org.jetbrains.compose.resources.stringResource
import swimresults.swimresultsapp.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
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
import org.koin.compose.viewmodel.koinViewModel

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
                    text = stringResource(Res.string.menShort), onClick = { onSwimmer(Swimmer.Men) })
                SwimFilterChip(selected = state.filters.swimmer == Swimmer.Women,
                    text = stringResource(Res.string.womenShort), onClick = { onSwimmer(Swimmer.Women) })
            }
            FilterGroup(stringResource(Res.string.poolGroup), modifier = Modifier.weight(1f)) {
                SwimFilterChip(selected = state.filters.poolType == PoolType.Short,
                    text = stringResource(Res.string.shortWaterLength), onClick = { onPool(PoolType.Short) })
                SwimFilterChip(selected = state.filters.poolType == PoolType.Long,
                    text = stringResource(Res.string.longWaterLength), onClick = { onPool(PoolType.Long) })
            }
        }
        val styles = listOf(SwimStyle.FREESTYLE, SwimStyle.BACKSTROKE,
            SwimStyle.BREASTSTROKE, SwimStyle.BUTTERFLY, SwimStyle.MEDLEY)
        FilterTabs(
            title = stringResource(Res.string.styleGroup),
            options = styles,
            selected = state.filters.swimStyle,
            label = { style -> stringResource(when (style) {
                SwimStyle.FREESTYLE -> Res.string.styleFreestyleShort
                SwimStyle.BACKSTROKE -> Res.string.styleBackstrokeShort
                SwimStyle.BREASTSTROKE -> Res.string.styleBreaststrokeShort
                SwimStyle.BUTTERFLY -> Res.string.styleButterflyShort
                SwimStyle.MEDLEY -> Res.string.styleMedleyShort
            }) },
            description = { styleName(it) },
            onSelect = onStyle
        )
        FilterTabs(
            title = stringResource(Res.string.distanceTitle),
            options = state.filters.distances,
            selected = state.filters.distance,
            label = { it.toString() },
            description = { stringResource(Res.string.distanceMeters, it) },
            onSelect = onDistance
        )
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
private fun <T> FilterTabs(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    description: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Theme.colors.textPrimary, modifier = Modifier.semantics { heading() })
        val minTabWidth = 48.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().clip(Theme.shapes.medium)
        ) {
            val rowWidth = maxOf(maxWidth, minTabWidth * options.size)
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                SecondaryTabRow(
                    selectedTabIndex = options.indexOf(selected).coerceAtLeast(0),
                    modifier = Modifier.width(rowWidth),
                    containerColor = Theme.colors.backGround2,
                    contentColor = Theme.colors.primary,
                    divider = {}
                ) {
                    options.forEach { option ->
                        val optionDescription = description(option)
                        Tab(
                            selected = option == selected,
                            onClick = { onSelect(option) },
                            modifier = Modifier.sizeIn(minHeight = 48.dp)
                                .semantics { contentDescription = optionDescription },
                            selectedContentColor = Theme.colors.textPrimary,
                            unselectedContentColor = Theme.colors.textSecondary
                        ) {
                            Text(
                                text = label(option),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                fontSize = 14.sp,
                                fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
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
