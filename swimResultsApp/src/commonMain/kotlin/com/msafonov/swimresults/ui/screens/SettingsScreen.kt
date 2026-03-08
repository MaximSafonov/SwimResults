package com.msafonov.swimresults.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msafonov.swimresults.ui.theme.Theme
import com.msafonov.swimresults.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import swimresults.swimresultsapp.generated.resources.Res
import swimresults.swimresultsapp.generated.resources.dartTheme
import swimresults.swimresultsapp.generated.resources.lightTheme

@Composable
fun SettingsScreen() {
    val viewModel = koinViewModel<SettingsViewModel>()
    val isDark by viewModel.isDark.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(Theme.colors.backGround)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
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
                text = stringResource(Res.string.lightTheme),
                textAlign = TextAlign.Center,
                color = Theme.colors.textPrimary
            )
            Switch(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                checked = isDark,
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
                text = stringResource(Res.string.dartTheme),
                textAlign = TextAlign.Center,
                color = Theme.colors.textPrimary
            )
        }

    }
}