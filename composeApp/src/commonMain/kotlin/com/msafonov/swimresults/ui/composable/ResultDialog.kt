package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.msafonov.swimresults.data.model.CheckedResult

@Composable
fun ResultDialog(
    onConfirmation: () -> Unit,
    result: CheckedResult?
) {
    val current = result?.currentRank?.text.orEmpty()
    val toNext =
        result?.nextRank?.text?.let { "До следующего (${it}):\n${result.toNextRank.toString()}" }
            ?: ""
    val betteThan = result?.toPreviousRank?.let { "ты лучше текущего на $it" } ?: "Есть куда расти"

    AlertDialog(
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                textAlign = TextAlign.Start,
                text = "Твой результат",
            )
        },
        text = {
            Text(
                textAlign = TextAlign.Start,
                text = "$current\n$toNext\n\n$betteThan"
            )
        },
        onDismissRequest = {
            onConfirmation()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("ПОНЯТНО")
            }
        },
    )
}

