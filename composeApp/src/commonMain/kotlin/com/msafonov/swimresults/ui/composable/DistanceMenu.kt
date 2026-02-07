package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
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
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.presentation.CheckResultViewModel
import com.msafonov.swimresults.ui.theme.chipColors
import org.jetbrains.compose.resources.painterResource
import swimresults.composeapp.generated.resources.Res
import swimresults.composeapp.generated.resources.compose_multiplatform

@Composable
fun DistanceMenu(distanceList: List<Int>, viewModel: CheckResultViewModel, distance: Int) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Text(
            textAlign = TextAlign.Center,
            text = "Дистанция"
        )
        Box(
            modifier = Modifier
                .padding(2.dp),
        ) {
            FilterChip(
                modifier = Modifier
                    .size(width = 80.dp, height = 40.dp),
                onClick = { expanded = !expanded },
                selected = true,
                colors = chipColors,
                label = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "$distance"
                    )
                },
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                distanceList.forEach { distance ->
                    DropdownMenuItem(
                        text = { Text("$distance") },
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