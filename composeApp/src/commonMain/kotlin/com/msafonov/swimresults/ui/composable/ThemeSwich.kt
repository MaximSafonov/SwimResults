package com.msafonov.swimresults.ui.composable

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.msafonov.swimresults.presentation.CheckResultViewModel

@Composable
fun SwitchTheme(isDark: Boolean, viewModel: CheckResultViewModel) {
    val isChecked = viewModel.isDarkTheme.collectAsState()
    var checked by remember { mutableStateOf(isDark) }

    Switch(
        checked = isChecked.value,
        onCheckedChange = { viewModel.updateTheme(it) }
    )
}