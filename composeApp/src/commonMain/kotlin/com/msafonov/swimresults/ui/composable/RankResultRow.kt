package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.TimeComponents

@Composable
fun RankResultRow(rank: SwimmingRank, time: TimeComponents) {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Text(
            modifier = Modifier
                .width(70.dp)
                .padding(start = 16.dp),
            text = rank.text,
            textAlign = TextAlign.Start
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            text = time.toRankFormat(),
            textAlign = TextAlign.End
        )
    }
}