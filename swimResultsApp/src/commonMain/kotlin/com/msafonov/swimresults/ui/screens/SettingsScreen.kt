package com.msafonov.swimresults.ui.screens

import org.jetbrains.compose.resources.stringResource
import swimresults.swimresultsapp.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val isDark by viewModel.isDark.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(Theme.colors.backGround)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back),
                    modifier = Modifier.size(32.dp), tint = Theme.colors.textPrimary)
            }
            Text(stringResource(Res.string.settingsScreenTitle), color = Theme.colors.textPrimary,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
        }
        Text(stringResource(Res.string.themeHeading), color = Theme.colors.textPrimary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), textAlign = TextAlign.Start)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(
                    onClick = { viewModel.toggleTheme() },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    color = Theme.colors.backGround2,
                    shape = Theme.shapes.medium
                ),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.lightTheme),
                textAlign = TextAlign.Center,
                color = Theme.colors.textPrimary
            )
            Switch(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                checked = isDark,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Theme.colors.textContrast,
                    uncheckedThumbColor = Theme.colors.textSecondary,
                    checkedTrackColor = Theme.colors.primary,
                    uncheckedTrackColor = Theme.colors.backGround2,
                    uncheckedBorderColor = Theme.colors.textSecondary
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
