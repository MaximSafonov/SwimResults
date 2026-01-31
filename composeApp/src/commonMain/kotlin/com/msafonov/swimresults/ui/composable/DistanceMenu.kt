package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.presentation.CheckResultViewModel
import org.jetbrains.compose.resources.painterResource
import swimresults.composeapp.generated.resources.Res
import swimresults.composeapp.generated.resources.compose_multiplatform

@Composable
fun DistanceMenu(distanceList: List<Int>, viewModel: CheckResultViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(16.dp),
    ) {
        IconButton(onClick = { expanded = !expanded }) {
            Image(painterResource(Res.drawable.compose_multiplatform), null)
        }
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