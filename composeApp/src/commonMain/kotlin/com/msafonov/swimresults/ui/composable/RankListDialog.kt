package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.SwimEvent
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.TimeComponents
import com.msafonov.swimresults.ui.theme.Blue30
import com.msafonov.swimresults.ui.theme.Blue40
import com.msafonov.swimresults.ui.theme.Ocean10
import com.msafonov.swimresults.ui.theme.Ocean8
import com.msafonov.swimresults.ui.theme.Red30

@Composable
fun RankListDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    event: SwimEvent,
    poolType: PoolType
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = " ${event.distance} ${event.style.text}",
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = 30.sp,
                        color = Ocean10
                    ),
                    modifier = Modifier.padding(bottom = 2.dp, top = 16.dp),
                )
                Text(
                    text = when(poolType) {
                        PoolType.Long -> "Длинная вода"
                        PoolType.Short -> "Короткая вода"
                    },
                    style = TextStyle(
                        fontSize = 20.sp,
                        color = Ocean8
                    ),
                    modifier = Modifier.padding(bottom = 32.dp, top = 2.dp),
                )
                event.results.forEach {
                    RankResultRow(it.value, TimeComponents.durationToComponents(it.key))
                    if (it.value.ordinal != SwimmingRank.entries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            color = Ocean10,
                            thickness = 1.dp
                        )
                    }

                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onConfirmation() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Понятно")
                    }
                }
            }
        }
    }
}

