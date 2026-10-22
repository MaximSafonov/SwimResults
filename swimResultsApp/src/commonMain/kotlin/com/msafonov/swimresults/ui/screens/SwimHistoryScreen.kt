package com.msafonov.swimresults.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import swimresults.swimresultsapp.generated.resources.Res
import swimresults.swimresultsapp.generated.resources.historyAdd
import swimresults.swimresultsapp.generated.resources.historyDistance
import swimresults.swimresultsapp.generated.resources.historyEmpty
import swimresults.swimresultsapp.generated.resources.historyLoadError
import swimresults.swimresultsapp.generated.resources.historyNoQualification
import swimresults.swimresultsapp.generated.resources.historyPool
import swimresults.swimresultsapp.generated.resources.historyQualification
import swimresults.swimresultsapp.generated.resources.historyTime
import swimresults.swimresultsapp.generated.resources.shortWaterLength
import swimresults.swimresultsapp.generated.resources.longWaterLength

@Composable
fun SwimHistoryScreen(
    onAddSwim: () -> Unit,
    viewModel: SwimHistoryViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(horizontalScrollState)
                                    .background(Theme.colors.backGround2, Theme.shapes.small)
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Cell("${swim.distanceMeters} м ${swim.style.text}", 176.dp)
                                Cell(stringResource(if (swim.poolLengthMeters == 25) Res.string.shortWaterLength else Res.string.longWaterLength), 72.dp)
                                Cell(formatTime(swim.timeCentiseconds), 92.dp)
                                Cell(swim.qualification?.text ?: stringResource(Res.string.historyNoQualification), 120.dp)
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
