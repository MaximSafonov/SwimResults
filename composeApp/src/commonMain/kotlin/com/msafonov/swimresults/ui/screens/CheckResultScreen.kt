package com.msafonov.swimresults.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.presentation.CheckResultViewModel
import com.msafonov.swimresults.ui.composable.DistanceMenu
import com.msafonov.swimresults.ui.composable.RankListDialog
import com.msafonov.swimresults.ui.composable.ResultDialog
import com.msafonov.swimresults.ui.composable.ResultUnput
import com.msafonov.swimresults.ui.composable.SwimmingStyles
import com.msafonov.swimresults.ui.theme.FunctionalDarkGrey
import com.msafonov.swimresults.ui.theme.Neutral3
import com.msafonov.swimresults.ui.theme.Ocean5
import com.msafonov.swimresults.ui.theme.Red90
import com.msafonov.swimresults.ui.theme.SwimRankTheme
import com.msafonov.swimresults.ui.theme.chipColors
import com.msafonov.swimresults.ui.theme.primaryButtonColors
import com.msafonov.swimresults.ui.theme.secondaryButtonColors

@Composable
@Preview
fun CheckResultScreen() {
    val viewModel = CheckResultViewModel()

    SwimRankTheme {
        var showResult by remember { mutableStateOf(false) }
        var showAll by remember { mutableStateOf(false) }

        val poolType by viewModel.poolType.collectAsState()
        val swimmer by viewModel.swimmer.collectAsState()
        val distance by viewModel.distance.collectAsState()
        val distanceList by viewModel.distanceList.collectAsState(emptyList())
        val result = viewModel.result.collectAsState()
        val event = viewModel.currentEvent.collectAsState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FunctionalDarkGrey)
        ) {
            Column(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
//            Row(
//                verticalAlignment = Alignment.Bottom,
//                modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer),
//            ) {
//                Text(
//                    text = "Темная",
//                    textAlign = TextAlign.Center,
//                    )
//                SwitchTheme(viewModel.isDarkTheme.value, viewModel)
//                Text(
//                    text = "Светлая",
//                    textAlign = TextAlign.Center,)
//
//            }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    FilterChip(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .size(width = 140.dp, height = 40.dp),
                        onClick = { viewModel.checkPool(PoolType.Short) },
                        selected = poolType == PoolType.Short,
                        colors = chipColors,
                        label = {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "25 м",
                                textAlign = TextAlign.Center
                            )
                        },
                    )
                    FilterChip(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .size(width = 140.dp, height = 40.dp),
                        onClick = { viewModel.checkPool(PoolType.Long) },
                        selected = poolType == PoolType.Long,
                        colors = chipColors,
                        label = {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                text = "50 м"
                            )
                        },
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    FilterChip(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .size(width = 140.dp, height = 40.dp),
                        onClick = { viewModel.setSwimmer(Swimmer.Women) },
                        selected = swimmer == Swimmer.Women,
                        colors = chipColors,
                        label = {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                text = "Девушки"
                            )
                        },
                    )
                    FilterChip(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .size(width = 140.dp, height = 40.dp),
                        onClick = { viewModel.setSwimmer(Swimmer.Men) },
                        selected = swimmer == Swimmer.Men,
                        colors = chipColors,
                        label = {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                text = "Юноши"
                            )
                        },
                    )
                }

                SwimmingStyles(viewModel = viewModel)
                DistanceMenu(distanceList, viewModel, distance)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    ResultUnput("мин", viewModel.minutes) { min -> viewModel.updateMinutes(min) }
                    ResultUnput("сек", viewModel.seconds) { sec -> viewModel.updateSeconds(sec) }
                    ResultUnput("сот", viewModel.milliseconds) { ms ->
                        viewModel.updateMilliseconds(ms)
                    }
                }

                Button(
                    onClick = {
                        showResult = !showResult
                        viewModel.checkResult()
                    },
                    colors = primaryButtonColors
                ) {
                    Text("Проверить результат")
                }
                AnimatedVisibility(showResult) {
                    ResultDialog(
                        onConfirmation = { showResult = !showResult },
                        result = result.value
                    )
                }

                Button(
                    onClick = { showAll = !showAll },
                    colors = secondaryButtonColors
                ) {
                    Text("Разряды по дистанции")
                }
                AnimatedVisibility(showAll) {
                    event.value?.let {
                        RankListDialog(
                            onDismissRequest = { },
                            onConfirmation = { showAll = !showAll },
                            event = it,
                            poolType = poolType
                        )
                    }
                }
            }
        }
    }
}