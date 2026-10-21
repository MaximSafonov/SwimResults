package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.msafonov.swimresults.ui.theme.Theme

@Composable
fun SwimFilterChip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Theme.colors.primary else Theme.colors.backGround2,
        contentColor = if (selected) Theme.colors.textContrast else Theme.colors.textPrimary,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) Theme.colors.primary else Theme.colors.textSecondary
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                textAlign = TextAlign.Center
            )
        }
    }
}
