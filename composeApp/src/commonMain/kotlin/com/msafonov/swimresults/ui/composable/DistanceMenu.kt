package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.msafonov.swimresults.viewmodel.CheckResultViewModel
import com.msafonov.swimresults.ui.theme.FunctionalGrey
import com.msafonov.swimresults.ui.theme.Theme

@Composable
fun DistanceMenu(distanceList: List<Int>, viewModel: CheckResultViewModel, distance: Int) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Text(
            modifier = Modifier.padding(end = 8.dp),
            textAlign = TextAlign.Center,
            text = "Дистанция",
            color = Theme.colors.textPrimary
        )
        Box(
            modifier = Modifier
                .padding(2.dp),
        ) {
            SwimFilterChip(
                modifier = Modifier
                    .size(width = 120.dp, height = 40.dp),
                onClick = { expanded = !expanded },
                selected = true,
                text = "$distance"
            )
            DropdownMenu(
                modifier = Modifier.width(120.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = Theme.colors.surface2,
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                distanceList.forEach { distance ->
                    DropdownMenuItem(
                        text = { Text(
                            text = "$distance",
                            color = Theme.colors.textPrimary
                        ) },
                        onClick = {
                            viewModel.setDistance(distance)
                            expanded = false
                        }
                    )
                }

            }
        }
    }

}