package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.ui.theme.Theme

@Composable
fun SwimFilterChip(
    modifier: Modifier,
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    FilterChip(
        modifier = modifier,
        onClick = onClick,
        selected = selected,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Theme.colors.surface1,
            selectedContainerColor = Theme.colors.surface3,
            labelColor = Theme.colors.textPrimary,
            selectedLabelColor = Theme.colors.textPrimary
        ),
        label = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = text,
                textAlign = TextAlign.Center
            )
        },
    )
}