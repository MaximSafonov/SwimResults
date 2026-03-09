package com.msafonov.swimresults.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.ui.composable.ButtonPrimary
import com.msafonov.swimresults.ui.composable.ButtonSecondary
import com.msafonov.swimresults.viewmodel.CheckResultViewModel
import com.msafonov.swimresults.ui.composable.DistanceMenu
import com.msafonov.swimresults.ui.dialogs.RankListDialog
import com.msafonov.swimresults.ui.dialogs.ResultDialog
import com.msafonov.swimresults.ui.composable.SwimFilterChip
import com.msafonov.swimresults.ui.composable.SwimmingStyles
import com.msafonov.swimresults.ui.composable.TimerInput
import com.msafonov.swimresults.ui.theme.Theme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import swimresults.swimresultsapp.generated.resources.Res
import swimresults.swimresultsapp.generated.resources.checkResult
import swimresults.swimresultsapp.generated.resources.distanceResults
import swimresults.swimresultsapp.generated.resources.longWaterLength
import swimresults.swimresultsapp.generated.resources.men
import swimresults.swimresultsapp.generated.resources.shortWaterLength
import swimresults.swimresultsapp.generated.resources.women

@Composable
@Preview
fun CheckResultScreen(
    passedResult: String? = null
) {
    val viewModel = koinViewModel<CheckResultViewModel>()
    var showResult by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }

    val poolType by viewModel.poolType.collectAsStateWithLifecycle()
    val swimmer by viewModel.swimmer.collectAsStateWithLifecycle()
    val distance by viewModel.distance.collectAsStateWithLifecycle()
    val distanceList by viewModel.distanceList.collectAsStateWithLifecycle(emptyList())
    val result = viewModel.result.collectAsStateWithLifecycle()
    val event = viewModel.currentEvent.collectAsStateWithLifecycle()

    val stopWatchTime by viewModel.stopWatchTime.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            stopWatchTime?.let {
                viewModel.onInputChanged(TextFieldValue(it))
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
                    text = stringResource(Res.string.shortWaterLength)
                )
                SwimFilterChip(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .size(width = 120.dp, height = 40.dp),
                    onClick = { viewModel.checkPool(PoolType.Long) },
                    selected = poolType == PoolType.Long,
                    text = stringResource(Res.string.longWaterLength)
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
                    text = stringResource(Res.string.women)
                )

                SwimFilterChip(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .size(width = 120.dp, height = 40.dp),
                    onClick = { viewModel.setSwimmer(Swimmer.Men) },
                    selected = swimmer == Swimmer.Men,
                    text = stringResource(Res.string.men)
                )
            }

            SwimmingStyles(
                modifier = Modifier.wrapContentWidth(),
                viewModel = viewModel
            )
            DistanceMenu(distanceList, viewModel, distance)
            TimerInput(viewModel)
            ButtonSecondary(
                text = stringResource(Res.string.checkResult),
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
                text = stringResource(Res.string.distanceResults)
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