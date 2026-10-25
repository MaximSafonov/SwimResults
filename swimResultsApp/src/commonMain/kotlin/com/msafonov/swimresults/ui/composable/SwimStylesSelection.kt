package com.msafonov.swimresults.ui.composable

import com.msafonov.swimresults.ui.strings.styleName
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.viewmodel.CheckResultViewModel
import com.msafonov.swimresults.ui.theme.Theme

@Composable
fun SwimmingStyles(modifier: Modifier = Modifier, viewModel: CheckResultViewModel) {
    val selected = viewModel.swimStyle.collectAsState()

    Column(modifier.selectableGroup()) {
        SwimStyle.entries.forEach { style ->
            Row(
                Modifier
                    .wrapContentWidth()
                    .height(32.dp)
                    .selectable(
                        selected = (style == selected.value),
                        onClick = { viewModel.setSwimStyle(style) },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (style == selected.value),
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Theme.colors.surface3,
                        unselectedColor = Theme.colors.surface1,
                    )
                )
                Text(
                    text = styleName(style),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp),
                    color = Theme.colors.textPrimary
                )
            }
        }
    }
}