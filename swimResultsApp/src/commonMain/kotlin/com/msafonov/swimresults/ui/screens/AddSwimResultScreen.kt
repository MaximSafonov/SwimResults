package com.msafonov.swimresults.ui.screens

import com.msafonov.swimresults.ui.strings.*
import org.jetbrains.compose.resources.stringResource
import swimresults.swimresultsapp.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.ui.composable.SwimFilterChip
import com.msafonov.swimresults.ui.composable.TimerInput
import com.msafonov.swimresults.ui.theme.Theme
import com.msafonov.swimresults.ui.theme.SwimComponentDefaults
import com.msafonov.swimresults.viewmodel.AddSwimResultViewModel
import com.msafonov.swimresults.viewmodel.AddSwimResultError
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddSwimResultScreen(
    onBack: () -> Unit,
    recordId: Long? = null,
    viewModel: AddSwimResultViewModel = koinViewModel()
) {
    val collectedState by viewModel.uiState.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val state = collectedState.takeIf { it.context == null || it.context.session == session }
        ?: com.msafonov.swimresults.viewmodel.AddSwimResultUiState(ready = false, error = AddSwimResultError.NotFound)
    LaunchedEffect(viewModel, recordId) { viewModel.initialize(recordId) }
    var formattedTime by remember(viewModel) { mutableStateOf(TextFieldValue(text = state.time)) }
    LaunchedEffect(state.time) {
        if (formattedTime.text != state.time) {
            formattedTime = TextFieldValue(
                text = state.time,
                selection = TextRange(state.time.length)
            )
        }
    }
    Column(
        Modifier.fillMaxSize().background(Theme.colors.backGround)
            .imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(if (recordId == null) Res.string.addSwimTitle else Res.string.editSwimTitle), color = Theme.colors.textPrimary,
                fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
            TextButton(onClick = onBack, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
                Text(stringResource(Res.string.cancel), color = Theme.colors.textPrimary)
            }
        }
        state.context?.let { Text(historyLabel(it.label), color = Theme.colors.textSecondary) }
        if (state.loading || !state.ready) {
            if (state.loading) {
                CircularProgressIndicator(color = Theme.colors.textPrimary)
                Text(stringResource(Res.string.swimRecordLoading), color = Theme.colors.textPrimary)
            } else {
                Text(stringResource(if (state.error == AddSwimResultError.NotFound)
                    Res.string.swimRecordNotFound else Res.string.swimRecordLoadError), color = Theme.colors.errorPrimary)
            }
            return@Column
        }
        CompetitionSelector(
            state = state,
            onSelect = viewModel::selectCompetition,
            onNew = viewModel::startNewCompetition
        )
        if (state.selectedCompetition == null) {
            OutlinedTextField(
                value = state.newCompetitionName,
                onValueChange = viewModel::setCompetitionName,
                modifier = Modifier.fillMaxWidth(),
                shape = Theme.shapes.small,
                colors = SwimComponentDefaults.outlinedTextFieldColors(),
                label = { Text(stringResource(Res.string.competitionNameLabel)) },
                singleLine = true
            )
        }
        DateField(state.swimDate, stringResource(Res.string.swimDateLabel), viewModel::setSwimDate)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectionGroup(stringResource(Res.string.genderGroup), Modifier.weight(1f)) {
                SwimFilterChip(selected = state.swimmer == Swimmer.Men, text = stringResource(Res.string.men)) { viewModel.setSwimmer(Swimmer.Men) }
                SwimFilterChip(selected = state.swimmer == Swimmer.Women, text = stringResource(Res.string.women)) { viewModel.setSwimmer(Swimmer.Women) }
            }
            SelectionGroup(stringResource(Res.string.poolGroup), Modifier.weight(1f)) {
                SwimFilterChip(selected = state.pool == PoolType.Short, text = stringResource(Res.string.shortWaterLength)) { viewModel.setPool(PoolType.Short) }
                SwimFilterChip(selected = state.pool == PoolType.Long, text = stringResource(Res.string.longWaterLength)) { viewModel.setPool(PoolType.Long) }
            }
        }
        SelectionGroup(stringResource(Res.string.styleGroup)) {
            SwimStyle.entries.forEach { style ->
                val label = stringResource(when (style) {
                    SwimStyle.FREESTYLE -> Res.string.styleFreestyle
                    SwimStyle.BACKSTROKE -> Res.string.styleBackstroke
                    SwimStyle.BREASTSTROKE -> Res.string.styleBreaststroke
                    SwimStyle.BUTTERFLY -> Res.string.styleButterfly
                    SwimStyle.MEDLEY -> Res.string.styleMedley
                })
                SwimFilterChip(selected = state.style == style, text = label) { viewModel.setStyle(style) }
            }
        }
        SelectionGroup(stringResource(Res.string.distanceTitle)) {
            state.distances.forEach { distance ->
                SwimFilterChip(selected = state.distance == distance,
                    text = stringResource(Res.string.distanceMeters, distance)) { viewModel.setDistance(distance) }
            }
        }
        TimerInput(
            value = formattedTime,
            onValueChange = { value ->
                if (!state.saving && !state.saved) {
                    formattedTime = value
                    viewModel.setTime(value.text)
                }
            }
        )
        state.error?.let {
            val message = when (it) {
                AddSwimResultError.LoadCompetitions -> Res.string.swimCompetitionsLoadError
                AddSwimResultError.SaveFailure -> Res.string.swimSaveFailure
                AddSwimResultError.Validation -> Res.string.swimSaveError
                AddSwimResultError.NotFound -> Res.string.swimRecordNotFound
                AddSwimResultError.LoadRecord -> Res.string.swimRecordLoadError
            }
            Text(stringResource(message), color = Theme.colors.errorPrimary)
        }
        Button(
            onClick = { viewModel.save(onBack) },
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
            colors = SwimComponentDefaults.primaryButtonColors()
        ) {
            if (state.saving) CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Theme.colors.textSecondary,
                strokeWidth = 2.dp
            )
            else Text(stringResource(Res.string.saveSwim))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompetitionSelector(
    state: com.msafonov.swimresults.viewmodel.AddSwimResultUiState,
    onSelect: (Long?) -> Unit,
    onNew: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.selectedCompetition?.let { stringResource(Res.string.competitionNameDate, it.name, formatDate(it.date)) }
        ?: state.newCompetitionName.ifBlank { stringResource(Res.string.competitionChoose) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = Theme.shapes.small,
            colors = SwimComponentDefaults.outlinedTextFieldColors(),
            label = { Text(stringResource(Res.string.competitionLabel)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = Theme.shapes.medium,
            containerColor = Theme.colors.backGround2
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.competitionNew)) },
                onClick = { onNew(); expanded = false }
            )
            state.competitions.forEach { competition ->
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.competitionNameDate, competition.name, formatDate(competition.date))) },
                    onClick = { onSelect(competition.id); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun SelectionGroup(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Theme.colors.textPrimary, modifier = Modifier.semantics { heading() })
        FlowRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) { content() }
    }
}

@Composable
private fun DateField(value: String, label: String, onChange: (String) -> Unit) {
    val display = formatDate(value)
    OutlinedTextField(
        value = display,
        onValueChange = { onChange(parseDate(it)) },
        modifier = Modifier.fillMaxWidth(),
        shape = Theme.shapes.small,
        colors = SwimComponentDefaults.outlinedTextFieldColors(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        supportingText = { if (display.isNotEmpty() && !isDate(display)) Text(stringResource(Res.string.dateFormatError)) }
    )
}

private fun formatDate(value: String): String {
    val parts = value.split('-')
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else value
}

private fun parseDate(value: String): String {
    val parts = value.split('.')
    return if (parts.size == 3 && parts[2].length == 4) "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}" else value
}

private fun isDate(value: String): Boolean = runCatching {
    val parts = value.split('.')
    require(parts.size == 3)
    kotlinx.datetime.LocalDate(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
}.isSuccess
