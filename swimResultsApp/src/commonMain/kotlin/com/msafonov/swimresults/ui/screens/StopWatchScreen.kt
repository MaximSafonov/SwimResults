package com.msafonov.swimresults.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msafonov.swimresults.ui.composable.ButtonPrimary
import com.msafonov.swimresults.ui.composable.ButtonSecondary
import com.msafonov.swimresults.ui.theme.Theme
import com.msafonov.swimresults.viewmodel.StopWatchViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import swimresults.swimresultsapp.generated.resources.Res
import swimresults.swimresultsapp.generated.resources.lap
import swimresults.swimresultsapp.generated.resources.lapCurrent
import swimresults.swimresultsapp.generated.resources.pause
import swimresults.swimresultsapp.generated.resources.reset
import swimresults.swimresultsapp.generated.resources.showResult
import swimresults.swimresultsapp.generated.resources.start

@Composable
fun StopWatchScreen(
    onCheckResult: () -> Unit
) {
    val viewModel = koinViewModel<StopWatchViewModel>()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                modifier = Modifier.padding(top = 56.dp, bottom = 56.dp),
                text = viewModel.formatTime(viewModel.elapsedTime),
                fontFamily = FontFamily.Monospace,
                color = Theme.colors.primary,
                fontSize = 56.sp
            )
        }

        item {
            ButtonSecondary(
                modifier = Modifier.padding(vertical = 8.dp),
                text = if (viewModel.isRunning) stringResource(Res.string.pause) else stringResource(
                    Res.string.start
                ),
                onClick = { if (viewModel.isRunning) viewModel.pause() else viewModel.start() })
        }

        item {
            ButtonPrimary(
                modifier = Modifier.padding(vertical = 8.dp),
                onClick = { viewModel.recordLap() },
                enabled = viewModel.isRunning,
                text = stringResource(Res.string.lap),
            )
        }

        item {
            ButtonPrimary(
                modifier = Modifier.padding(vertical = 8.dp),
                onClick = { viewModel.reset() },
                text = stringResource(Res.string.reset)
            )

        }

        itemsIndexed(viewModel.laps.reversed()) { index, lap ->
            Text(
                modifier = Modifier.padding(4.dp),
                text = stringResource(Res.string.lapCurrent, index + 1, viewModel.formatTime(lap)),
                color = Theme.colors.textPrimary
            )
        }

        item {
            ButtonSecondary(
                modifier = Modifier.padding(vertical = 8.dp),
                text = stringResource(Res.string.showResult),
                onClick = {
                    viewModel.setTime()
                    onCheckResult()
                }
            )
        }
    }
}