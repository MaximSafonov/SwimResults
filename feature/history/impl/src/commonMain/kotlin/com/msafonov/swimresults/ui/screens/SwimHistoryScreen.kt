package com.msafonov.swimresults.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.msafonov.swimresults.historyShareText
import com.msafonov.swimresults.rememberHistoryShareLauncher
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msafonov.swimresults.data.model.SwimCompetitionGroup
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.WorldAquaticsPoints
import com.msafonov.swimresults.ui.theme.Theme
import com.msafonov.swimresults.viewmodel.SwimHistoryViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import swimresults.feature.history.impl.generated.resources.historyEdit
import swimresults.feature.history.impl.generated.resources.historyShare
import swimresults.feature.history.impl.generated.resources.historyDelete
import swimresults.feature.history.impl.generated.resources.historyDeleteError
import swimresults.feature.history.impl.generated.resources.historyShareError
import swimresults.feature.history.impl.generated.resources.Res
import swimresults.feature.history.impl.generated.resources.historyAdd
import swimresults.feature.history.impl.generated.resources.historyDistance
import swimresults.feature.history.impl.generated.resources.historyEmpty
import swimresults.feature.history.impl.generated.resources.historyLoadError
import swimresults.feature.history.impl.generated.resources.historyNoQualification
import swimresults.feature.history.impl.generated.resources.historyPool
import swimresults.feature.history.impl.generated.resources.historyQualification
import swimresults.feature.history.impl.generated.resources.historyTime
import swimresults.feature.history.impl.generated.resources.historyFina
import swimresults.feature.history.impl.generated.resources.historyFinaUnavailable
import swimresults.feature.history.impl.generated.resources.historyDistanceValue
import swimresults.feature.history.impl.generated.resources.styleFreestyleShort
import swimresults.feature.history.impl.generated.resources.styleBackstrokeShort
import swimresults.feature.history.impl.generated.resources.styleBreaststrokeShort
import swimresults.feature.history.impl.generated.resources.styleButterflyShort
import swimresults.feature.history.impl.generated.resources.styleMedleyShort
import swimresults.feature.history.impl.generated.resources.shortWaterLength
import swimresults.feature.history.impl.generated.resources.longWaterLength

@Composable
fun SwimHistoryScreen(
    onAddSwim: () -> Unit,
    onEditSwim: (Long) -> Unit,
    viewModel: SwimHistoryViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuRecordId by remember { mutableStateOf<Long?>(null) }
    var pendingShare by remember { mutableStateOf<String?>(null) }
    var shareError by remember { mutableStateOf(false) }
    val share = rememberHistoryShareLauncher()
    LaunchedEffect(state.groups) {
        if (state.groups.none { group -> group.swims.any { it.id == menuRecordId } }) menuRecordId = null
    }
    LaunchedEffect(pendingShare) {
        val text = pendingShare ?: return@LaunchedEffect
        try {
            // Let the dropdown leave composition before opening the system UI.
            withFrameNanos { }
            share(text)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            shareError = true
        } finally {
            pendingShare = null
        }
    }
    val horizontalScrollState = rememberScrollState()
    Scaffold(
        containerColor = Theme.colors.backGround,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSwim,
                shape = Theme.shapes.medium,
                containerColor = Theme.colors.primary,
                contentColor = Theme.colors.textContrast
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.historyAdd))
            }
        }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (state.actionError) Text(stringResource(Res.string.historyDeleteError), color = Theme.colors.errorPrimary)
            if (shareError) Text(stringResource(Res.string.historyShareError), color = Theme.colors.errorPrimary)
            HistoryTableHeader(horizontalScrollState)
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Theme.colors.textPrimary)
                }
                state.error -> Text(stringResource(Res.string.historyLoadError), color = Theme.colors.textPrimary)
                state.groups.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.historyEmpty), color = Theme.colors.textSecondary)
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.groups.forEach { group ->
                        item(key = "competition-${group.competition.id}") {
                            CompetitionHeading(group)
                        }
                        items(group.swims, key = { "swim-${it.id}" }) { swim ->
                            val shareText = historyShareText(group.competition, swim)
                            val available = swim.id !in state.deletingIds && pendingShare == null
                            val style = stringResource(when (swim.style) {
                                SwimStyle.FREESTYLE -> Res.string.styleFreestyleShort
                                SwimStyle.BACKSTROKE -> Res.string.styleBackstrokeShort
                                SwimStyle.BREASTSTROKE -> Res.string.styleBreaststrokeShort
                                SwimStyle.BUTTERFLY -> Res.string.styleButterflyShort
                                SwimStyle.MEDLEY -> Res.string.styleMedleyShort
                            })
                            val pool = when (swim.poolLengthMeters) {
                                25 -> PoolType.Short
                                50 -> PoolType.Long
                                else -> null
                            }
                            val fina = pool?.let {
                                WorldAquaticsPoints.score(it, swim.gender, swim.style, swim.distanceMeters, swim.timeCentiseconds)
                            }
                            Box(Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(horizontalScrollState)
                                        .background(Theme.colors.backGround2, Theme.shapes.small)
                                        .clickable(enabled = available) { menuRecordId = swim.id }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Cell(stringResource(Res.string.historyDistanceValue, swim.distanceMeters, style), 100.dp)
                                    Cell(stringResource(if (swim.poolLengthMeters == 25) Res.string.shortWaterLength else Res.string.longWaterLength), 64.dp)
                                    Cell(formatTime(swim.timeCentiseconds), 80.dp)
                                    Cell(swim.qualification?.text ?: stringResource(Res.string.historyNoQualification), 100.dp)
                                    Cell(fina?.toString() ?: stringResource(Res.string.historyFinaUnavailable), 64.dp)
                                }
                                DropdownMenu(
                                    expanded = menuRecordId == swim.id && available,
                                    onDismissRequest = { menuRecordId = null },
                                    shape = Theme.shapes.medium,
                                    containerColor = Theme.colors.backGround2
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.historyEdit), color = Theme.colors.textPrimary) },
                                        onClick = { menuRecordId = null; onEditSwim(swim.id) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.historyShare), color = Theme.colors.textPrimary) },
                                        onClick = {
                                            menuRecordId = null
                                            shareError = false
                                            pendingShare = shareText
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
}

@Composable
private fun HistoryTableHeader(horizontalScrollState: androidx.compose.foundation.ScrollState) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(horizontalScrollState).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell(stringResource(Res.string.historyDistance), 100.dp)
        HeaderCell(stringResource(Res.string.historyPool), 64.dp)
        HeaderCell(stringResource(Res.string.historyTime), 80.dp)
        HeaderCell(stringResource(Res.string.historyQualification), 100.dp)
        HeaderCell(stringResource(Res.string.historyFina), 64.dp)
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
