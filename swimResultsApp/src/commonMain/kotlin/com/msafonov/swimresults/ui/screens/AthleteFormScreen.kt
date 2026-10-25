package com.msafonov.swimresults.ui.screens

import org.jetbrains.compose.resources.stringResource
import swimresults.swimresultsapp.generated.resources.*
import com.msafonov.swimresults.ui.strings.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.ui.theme.Theme
import com.msafonov.swimresults.viewmodel.AthleteFormViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AthleteFormScreen(onBack: () -> Unit, athleteId: Long? = null, viewModel: AthleteFormViewModel = koinViewModel()) {
    val collectedState by viewModel.uiState.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val state = collectedState.takeIf { viewModel.isCurrent(session) }
        ?: com.msafonov.swimresults.viewmodel.AthleteFormUiState(loading = false, validSession = false)
    var exitDialog by rememberSaveable { mutableStateOf(false) }
    var rankExpanded by rememberSaveable(athleteId, session) { mutableStateOf(false) }
    LaunchedEffect(viewModel, athleteId) { viewModel.initialize(athleteId) }
    val requestBack: () -> Unit = {
        if (!state.saving) { if (state.dirty && state.validSession) exitDialog = true else onBack() }
    }
    // A full-window common dialog owns system back as well as the toolbar action,
    // without adding platform back-handler APIs to commonMain.
    Dialog(onDismissRequest = requestBack, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Box(Modifier.fillMaxSize().background(Theme.colors.backGround), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (athleteId == null) stringResource(Res.string.athleteNewTitle) else stringResource(Res.string.athleteEditTitle), color = Theme.colors.textPrimary, fontSize = 22.sp)
                TextButton(onClick = requestBack, enabled = !state.saving, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.back)) }
                if (state.loading) CircularProgressIndicator()
                else if (state.validSession) {
                    OutlinedTextField(value = state.name, onValueChange = viewModel::name, label = { Text(stringResource(Res.string.athleteNameLabel)) },
                        supportingText = { Text(stringResource(Res.string.athleteNameHint)) }, enabled = !state.saving, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = state.birthDate, onValueChange = viewModel::birthDate, label = { Text(stringResource(Res.string.athleteBirthDateLabel)) },
                        supportingText = { Text(stringResource(if (state.validBirthDate) Res.string.athleteBirthDateHint else Res.string.athleteBirthDateError)) },
                        isError = !state.validBirthDate, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true, enabled = !state.saving, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(Res.string.athleteRankLabel), color = Theme.colors.textPrimary)
                    Box {
                        OutlinedButton(onClick = { rankExpanded = true }, enabled = !state.saving,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(state.rank?.let { rankFullName(it) } ?: stringResource(Res.string.athleteRankUnspecified))
                        }
                        DropdownMenu(expanded = rankExpanded && !state.saving, onDismissRequest = { rankExpanded = false }) {
                            DropdownMenuItem(text = { Text(stringResource(Res.string.athleteRankUnspecified)) },
                                onClick = { viewModel.rank(null); rankExpanded = false })
                            SwimmingRank.entries.forEach { rank ->
                                DropdownMenuItem(text = { Text(rankFullName(rank)) }, onClick = { viewModel.rank(rank); rankExpanded = false })
                            }
                        }
                    }
                    Text(stringResource(Res.string.athleteGenderLabel), color = Theme.colors.textPrimary)
                    listOf(null, Swimmer.Men, Swimmer.Women).forEach { value ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.gender == value, enabled = !state.saving, onClick = { viewModel.gender(value) })
                            TextButton(onClick = { viewModel.gender(value) }, enabled = !state.saving, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(when (value) { null -> stringResource(Res.string.sportsGenderUnspecified); Swimmer.Men -> stringResource(Res.string.sportsGenderMen); Swimmer.Women -> stringResource(Res.string.sportsGenderWomen) })
                            }
                        }
                    }
                    Button(onClick = { viewModel.save(onBack) }, enabled = state.valid && !state.saving && !state.saved,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (state.saving) stringResource(Res.string.saving) else stringResource(Res.string.saveSwim)) }
                } else Text(stringResource(Res.string.athleteFormUnavailable), color = Theme.colors.textPrimary)
                state.error?.let { Text(athleteFormError(it), color = Theme.colors.errorPrimary) }
            }
        }
        if (exitDialog && state.validSession) AlertDialog(onDismissRequest = { exitDialog = false }, title = { Text(stringResource(Res.string.athleteSaveChangesTitle)) },
            text = { Text(stringResource(Res.string.athleteUnsavedChanges)) },
            confirmButton = { TextButton(onClick = { exitDialog = false; viewModel.save(onBack) }, enabled = state.valid && !state.saving) { Text(stringResource(Res.string.saveSwim)) } },
            dismissButton = { Column {
                TextButton(onClick = { exitDialog = false }) { Text(stringResource(Res.string.athleteContinueEditing)) }
                TextButton(onClick = { exitDialog = false; onBack() }) { Text(stringResource(Res.string.athleteDiscard)) }
            } })
    }
}
