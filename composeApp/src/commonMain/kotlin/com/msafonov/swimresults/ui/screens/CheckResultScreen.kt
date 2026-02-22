package com.msafonov.swimresults.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.msafonov.swimresults.ui.composable.ButtonPrimary
import com.msafonov.swimresults.ui.composable.ButtonSecondary
import com.msafonov.swimresults.viewmodel.CheckResultViewModel
import com.msafonov.swimresults.ui.composable.DistanceMenu
import com.msafonov.swimresults.ui.composable.RankListDialog
import com.msafonov.swimresults.ui.composable.ResultDialog
import com.msafonov.swimresults.ui.composable.SwimFilterChip
import com.msafonov.swimresults.ui.composable.SwimmingStyles
import com.msafonov.swimresults.ui.composable.TimerInput
import com.msafonov.swimresults.ui.theme.SwimRankAppTheme
import com.msafonov.swimresults.ui.theme.Theme

@Composable
@Preview
fun CheckResultScreen(viewModel: CheckResultViewModel) {

    SwimRankAppTheme(viewModel.isDark) {
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
                .background(Theme.colors.backGround)
        ) {
            Column(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(
                            onClick = { viewModel.toggleTheme() },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(
                            color = Theme.colors.surface1,
                            shape = RoundedCornerShape(8.dp)
                        ),
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Светлая тема",
                        textAlign = TextAlign.Center,
                        color = Theme.colors.textPrimary
                    )
                    Switch(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        checked = viewModel.isDark,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Theme.colors.surface1,
                            uncheckedThumbColor = Theme.colors.support60,
                            checkedTrackColor = Theme.colors.surface3,
                            uncheckedTrackColor = Theme.colors.support20,
                            uncheckedBorderColor = Theme.colors.surface3
                        )
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Темная тема",
                        textAlign = TextAlign.Center,
                        color = Theme.colors.textPrimary
                    )
                }
                Spacer(Modifier.fillMaxWidth().height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SwimFilterChip(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .size(width = 120.dp, height = 40.dp),
                        onClick = { viewModel.checkPool(PoolType.Short) },
                        selected = poolType == PoolType.Short,
                        text = "25 м"
                    )
                    SwimFilterChip(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .size(width = 120.dp, height = 40.dp),
                        onClick = { viewModel.checkPool(PoolType.Long) },
                        selected = poolType == PoolType.Long,
                        text = "50 м"
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SwimFilterChip(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .size(width = 120.dp, height = 40.dp),
                        onClick = { viewModel.setSwimmer(Swimmer.Women) },
                        selected = swimmer == Swimmer.Women,
                        text = "Девушки"
                    )

                    SwimFilterChip(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .size(width = 120.dp, height = 40.dp),
                        onClick = { viewModel.setSwimmer(Swimmer.Men) },
                        selected = swimmer == Swimmer.Men,
                        text = "Юноши"
                    )
                }

                SwimmingStyles(
                    modifier = Modifier.wrapContentWidth(),
                    viewModel = viewModel)
                DistanceMenu(distanceList, viewModel, distance)
                TimerInput(viewModel)
                ButtonSecondary(
                    text = "Проверить результат",
                    onClick = {
                        showResult = !showResult
                        viewModel.checkResult()
                    },
                )
                if (showResult) {
                    ResultDialog(
                        onConfirmation = { showResult = !showResult },
                        result = result.value
                    )
                }
                Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))
                ButtonPrimary(
                    onClick = { showAll = !showAll },
                    text = "Разряды по дистанции"
                )
                if (showAll) {
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