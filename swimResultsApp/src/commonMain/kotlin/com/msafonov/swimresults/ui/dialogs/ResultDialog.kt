package com.msafonov.swimresults.ui.dialogs

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.msafonov.swimresults.data.model.CheckedResult
import com.msafonov.swimresults.ui.theme.Theme
import org.jetbrains.compose.resources.stringResource
import swimresults.swimresultsapp.generated.resources.Res
import swimresults.swimresultsapp.generated.resources.betterThanCurrent
import swimresults.swimresultsapp.generated.resources.close
import swimresults.swimresultsapp.generated.resources.goBetter
import swimresults.swimresultsapp.generated.resources.rank
import swimresults.swimresultsapp.generated.resources.toNextRank

@Composable
fun ResultDialog(
    onConfirmation: () -> Unit,
    result: CheckedResult?
) {
    val current = result?.currentRank?.text.orEmpty()
    val toNext =
        result?.nextRank?.text?.let { stringResource(Res.string.toNextRank,it, result.toNextRank.toString()) }
            ?: ""
    val betteThan = result?.toPreviousRank?.let { stringResource(Res.string.betterThanCurrent, it) } ?: stringResource(Res.string.goBetter)

    AlertDialog(
        containerColor = Theme.colors.primary,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                textAlign = TextAlign.Start,
                text = stringResource(Res.string.rank, current),
                color = Theme.colors.onPrimary
            )
        },
        text = {
            Text(
                textAlign = TextAlign.Start,
                text = "$toNext\n\n$betteThan",
                color = Theme.colors.onPrimary
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
                Text(
                    color = Theme.colors.onPrimary,
                    text = stringResource(Res.string.close)
                )
            }
        },
    )
}

