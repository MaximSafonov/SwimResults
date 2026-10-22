package com.msafonov.swimresults.ui.screens

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
import com.msafonov.swimresults.viewmodel.AddSwimResultViewModel
import com.msafonov.swimresults.viewmodel.AddSwimResultError
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import swimresults.swimresultsapp.generated.resources.Res
import swimresults.swimresultsapp.generated.resources.addSwimTitle
import swimresults.swimresultsapp.generated.resources.cancel
import swimresults.swimresultsapp.generated.resources.competitionChoose
import swimresults.swimresultsapp.generated.resources.competitionLabel
import swimresults.swimresultsapp.generated.resources.competitionNameLabel
import swimresults.swimresultsapp.generated.resources.competitionNew
import swimresults.swimresultsapp.generated.resources.dateFormatError
import swimresults.swimresultsapp.generated.resources.distanceMeters
import swimresults.swimresultsapp.generated.resources.distanceTitle
import swimresults.swimresultsapp.generated.resources.genderGroup
import swimresults.swimresultsapp.generated.resources.swimSaveFailure
import swimresults.swimresultsapp.generated.resources.swimSaveError
import swimresults.swimresultsapp.generated.resources.swimCompetitionsLoadError
import swimresults.swimresultsapp.generated.resources.longWaterLength
import swimresults.swimresultsapp.generated.resources.poolGroup
import swimresults.swimresultsapp.generated.resources.saveSwim
import swimresults.swimresultsapp.generated.resources.shortWaterLength
import swimresults.swimresultsapp.generated.resources.styleGroup
import swimresults.swimresultsapp.generated.resources.swimDateLabel
import swimresults.swimresultsapp.generated.resources.men
import swimresults.swimresultsapp.generated.resources.women
import swimresults.swimresultsapp.generated.resources.styleFreestyle
import swimresults.swimresultsapp.generated.resources.styleBackstroke
import swimresults.swimresultsapp.generated.resources.styleBreaststroke
import swimresults.swimresultsapp.generated.resources.styleButterfly
import swimresults.swimresultsapp.generated.resources.styleMedley

@Composable
fun AddSwimResultScreen(
    onBack: () -> Unit,
    viewModel: AddSwimResultViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(Res.string.addSwimTitle), color = Theme.colors.textPrimary,
                fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
            TextButton(onClick = onBack, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
                Text(stringResource(Res.string.cancel), color = Theme.colors.textPrimary)
            }
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
                formattedTime = value
                viewModel.setTime(value.text)
            }
        )
        state.error?.let {
            val message = when (it) {
                AddSwimResultError.LoadCompetitions -> Res.string.swimCompetitionsLoadError
                AddSwimResultError.SaveFailure -> Res.string.swimSaveFailure
                AddSwimResultError.Validation -> Res.string.swimSaveError
            }
            Text(stringResource(message), color = Theme.colors.textPrimary)
        }
        Button(
            onClick = { viewModel.save(onBack) },
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
        ) {
            if (state.saving) CircularProgressIndicator()
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
    val selected = state.selectedCompetition?.let { "${it.name} (${formatDate(it.date)})" }
        ?: state.newCompetitionName.ifBlank { stringResource(Res.string.competitionChoose) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            label = { Text(stringResource(Res.string.competitionLabel)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.competitionNew)) },
                onClick = { onNew(); expanded = false }
            )
            state.competitions.forEach { competition ->
                DropdownMenuItem(
                    text = { Text("${competition.name} (${formatDate(competition.date)})") },
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
