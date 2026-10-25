package com.msafonov.swimresults.ui.screens

import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import swimresults.swimresultsapp.generated.resources.*
import com.msafonov.swimresults.ui.strings.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.key
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import com.msafonov.swimresults.data.model.HistoryContext
import com.msafonov.swimresults.viewmodel.SwimHistoryUiState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CancellationException
import com.msafonov.swimresults.formatHistoryShareText
import com.msafonov.swimresults.rememberHistoryShareLauncher
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msafonov.swimresults.data.model.SwimCompetitionGroup
import com.msafonov.swimresults.ui.theme.Theme
import com.msafonov.swimresults.viewmodel.SwimHistoryViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SwimHistoryScreen(
    onAddSwim: () -> Unit,
    onEditSwim: (Long) -> Unit,
    onProfile: () -> Unit,
    viewModel: SwimHistoryViewModel = koinViewModel()
) {
    val collectedState by viewModel.uiState.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val state = collectedState.takeIf { it.context?.session == session } ?: SwimHistoryUiState()
    var menuRecordId by remember { mutableStateOf<Long?>(null) }
    var pendingShare by remember { mutableStateOf<Pair<Long, HistoryContext>?>(null) }
    var shareError by remember { mutableStateOf(false) }
    val share = rememberHistoryShareLauncher()
    LaunchedEffect(state.context, state.selecting) {
        menuRecordId = null
        pendingShare = null
        shareError = false
    }
    LaunchedEffect(state.groups) {
        if (state.groups.none { group -> group.swims.any { it.id == menuRecordId } }) menuRecordId = null
    }
    LaunchedEffect(pendingShare, state.context) {
        val request = pendingShare ?: return@LaunchedEffect
        try {
            // Let the dropdown leave composition before opening the system UI.
            withFrameNanos { }
            val record = viewModel.shareRecord(request.first, request.second)
            if (record != null && viewModel.isCurrent(request.second)) {
                val text = formatHistoryShareText(record.second, record.first)
                // Resource resolution suspends; recheck ownership immediately before sharing.
                if (viewModel.isCurrent(request.second)) share(text)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            shareError = true
        } finally {
            pendingShare = null
        }
    }
    val horizontalScrollState = rememberScrollState()
    val athleteScrollState = rememberScrollState()
    val historyListState = rememberLazyListState()
    Scaffold(
        containerColor = Theme.colors.backGround,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (state.context?.available == true && !state.selecting && !state.linking && !state.choosingAthlete) onAddSwim() },
                shape = Theme.shapes.medium,
                containerColor = Theme.colors.primary,
                contentColor = Theme.colors.textContrast
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.historyAdd))
            }
        }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(state.context?.let { historyLabel(it.label) } ?: stringResource(Res.string.loading), color = Theme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
            if (state.context?.session?.accountId != null) {
                Row(Modifier.fillMaxWidth().horizontalScroll(athleteScrollState).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.athletes.forEach { athlete ->
                        key(athlete.id) {
                            FilterChip(selected = state.context?.available == true && state.context.athleteId == athlete.id,
                                onClick = { viewModel.selectAthlete(athlete.id) },
                                enabled = !state.loading && !state.error && !state.linking && !state.choosingAthlete,
                                label = { Text(athlete.name) }, modifier = Modifier.heightIn(min = 48.dp))
                        }
                    }
                }
                TextButton(onClick = { viewModel.deviceLocal(true) },
                    enabled = !state.loading && !state.error && !state.linking && !state.choosingAthlete,
                    modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.historyWithoutAthlete)) }
            }
            if (state.context?.available == false) TextButton(onClick = onProfile) { Text(stringResource(Res.string.historyChooseAthlete)) }
            if (state.canLink && !state.selecting && !state.choosingAthlete) TextButton(onClick = viewModel::startSelection) { Text(stringResource(Res.string.historySelectRecords)) }
            if (state.selecting) {
                Text(stringResource(Res.string.historySelectedCount, state.selectedIds.size, 200), color = Theme.colors.textPrimary)
                TextButton(onClick = viewModel::cancelSelection, enabled = !state.linking) { Text(stringResource(Res.string.historyCancelSelection)) }
                if (state.athletes.isEmpty()) TextButton(onClick = { viewModel.cancelSelection(); onProfile() }) { Text(stringResource(Res.string.historyCreateAthlete)) }
                Column(Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState())) {
                    state.athletes.forEach { athlete ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.targetId == athlete.id, onClick = { viewModel.target(athlete.id) }, enabled = !state.linking)
                            TextButton(onClick = { viewModel.target(athlete.id) }, enabled = !state.linking) { Text(athlete.name) }
                        }
                    }
                }
                TextButton(onClick = viewModel::prepareLink, enabled = state.selectedIds.isNotEmpty() && state.targetId != null && !state.linking) { Text(stringResource(Res.string.historyPrepareLink)) }
            }
            state.message?.let { Text(historyMessage(it), color = Theme.colors.textPrimary) }
            if (state.actionError) Text(stringResource(Res.string.historyActionError), color = Theme.colors.errorPrimary)
            if (shareError) Text(stringResource(Res.string.historyShareError), color = Theme.colors.errorPrimary)
            HistoryTableHeader(horizontalScrollState)
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Theme.colors.textPrimary)
                }
                state.error -> Column {
                    Text(stringResource(Res.string.historyLoadError), color = Theme.colors.textPrimary)
                    TextButton(onClick = viewModel::retry, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.historyRetry)) }
                }
                state.groups.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.historyEmpty), color = Theme.colors.textSecondary)
                }
                else -> LazyColumn(state = historyListState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.groups.forEach { group ->
                        item(key = "competition-${group.competition.id}", contentType = "competition") {
                            CompetitionHeading(group)
                        }
                        items(group.swims, key = { "swim-${it.id}" }, contentType = { "swim" }) { swim ->
                            val available = swim.id !in state.deletingIds && pendingShare == null && !state.choosingAthlete
                            Box(Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(horizontalScrollState)
                                        .background(Theme.colors.backGround2, Theme.shapes.small)
                                        .clickable(enabled = available && !state.linking) { if (state.selecting) viewModel.toggle(swim.id) else menuRecordId = swim.id }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (state.selecting) Checkbox(checked = swim.id in state.selectedIds, enabled = !state.linking,
                                        onCheckedChange = { viewModel.toggle(swim.id) })
                                    Cell(stringResource(Res.string.distanceStyle, swim.distanceMeters, styleName(swim.style)), 176.dp)
                                    Cell(stringResource(if (swim.poolLengthMeters == 25) Res.string.shortWaterLength else Res.string.longWaterLength), 72.dp)
                                    Cell(formatTime(swim.timeCentiseconds), 92.dp)
                                    Cell(swim.qualification?.let { rankShortName(it) } ?: stringResource(Res.string.historyNoQualification), 120.dp)
                                }
                                DropdownMenu(
                                    expanded = !state.selecting && menuRecordId == swim.id && available,
                                    onDismissRequest = { menuRecordId = null },
                                    shape = Theme.shapes.medium,
                                    containerColor = Theme.colors.backGround2
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.historyEdit), color = Theme.colors.textPrimary) },
                                        onClick = { menuRecordId = null; state.context?.let { if (viewModel.isCurrent(it)) onEditSwim(swim.id) } }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.historyShare), color = Theme.colors.textPrimary) },
                                        onClick = {
                                            menuRecordId = null
                                            shareError = false
                                            pendingShare = state.context?.let { swim.id to it }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.historyDelete), color = Theme.colors.errorPrimary) },
                                        onClick = { menuRecordId = null; viewModel.delete(swim.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val confirmation = state.confirmation
    if (confirmation != null) AlertDialog(
        onDismissRequest = viewModel::dismissConfirmation,
        title = { Text(pluralStringResource(Res.plurals.historyLinkTitle, confirmation.rows.size, confirmation.rows.size)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(confirmation.target.name, fontWeight = FontWeight.Bold)
                Text(athleteMetadata(confirmation.target.birthDate, confirmation.target.rank, confirmation.target.sportsGender))
                Text(stringResource(Res.string.historyLinkDescription))
                confirmation.rows.forEach { swim ->
                    Text(stringResource(Res.string.historyRecordSummary, swim.swimDate, swim.distanceMeters, styleName(swim.style), formatTime(swim.timeCentiseconds)))
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::confirmLink, enabled = !state.linking) { Text(if (state.linking) stringResource(Res.string.historyLinking) else pluralStringResource(Res.plurals.historyLinkAction, confirmation.rows.size, confirmation.rows.size)) } },
        dismissButton = { TextButton(onClick = viewModel::dismissConfirmation, enabled = !state.linking) { Text(stringResource(Res.string.cancel)) } }
    )
}

@Composable
private fun HistoryTableHeader(horizontalScrollState: androidx.compose.foundation.ScrollState) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(horizontalScrollState).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell(stringResource(Res.string.historyDistance), 176.dp)
        HeaderCell(stringResource(Res.string.historyPool), 72.dp)
        HeaderCell(stringResource(Res.string.historyTime), 92.dp)
        HeaderCell(stringResource(Res.string.historyQualification), 120.dp)
    }
}

@Composable
private fun CompetitionHeading(group: SwimCompetitionGroup) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp)) {
        Text(group.competition.name, color = Theme.colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text(formatShortDate(group.competition.date), color = Theme.colors.textSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(text, Modifier.width(width).padding(horizontal = 4.dp), color = Theme.colors.textSecondary,
        fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun Cell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(text, Modifier.width(width).padding(horizontal = 4.dp), color = Theme.colors.textPrimary,
        fontSize = 14.sp, textAlign = TextAlign.Start)
}

private fun formatShortDate(iso: String): String {
    val parts = iso.split('-')
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0].takeLast(2)}" else iso
}

private fun formatTime(centiseconds: Long): String {
    val totalSeconds = centiseconds / 100
    val minutes = totalSeconds / 60
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    val fraction = (centiseconds % 100).toString().padStart(2, '0')
    return if (minutes > 0) "$minutes:$seconds.$fraction" else "$seconds.$fraction"
}
