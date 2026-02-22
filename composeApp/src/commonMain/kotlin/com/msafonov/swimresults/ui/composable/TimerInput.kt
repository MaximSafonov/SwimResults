package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msafonov.swimresults.ui.theme.Theme
import com.msafonov.swimresults.viewmodel.CheckResultViewModel
import org.jetbrains.compose.resources.painterResource
import swimresults.composeapp.generated.resources.Res
import swimresults.composeapp.generated.resources.swim_results_close

@Composable
fun TimerInput(
    viewModel: CheckResultViewModel
) {
    val textFieldValue by viewModel.timerInput.collectAsStateWithLifecycle()

    TextField(
        modifier = Modifier.fillMaxWidth().height(84.dp)
            .padding(vertical = 16.dp, horizontal = 24.dp),
        textStyle = TextStyle(
            color = Theme.colors.textPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp
        ),
        colors = TextFieldDefaults.colors(
            focusedTextColor = Theme.colors.textPrimary,
            unfocusedTextColor = Theme.colors.surface3,
            focusedIndicatorColor = Theme.colors.surface2,
            unfocusedIndicatorColor = Theme.colors.surface1,
            cursorColor = Theme.colors.surface3,
            focusedContainerColor = Theme.colors.surface2,
            unfocusedContainerColor = Theme.colors.surface1,
            focusedLabelColor = Theme.colors.primary,
            unfocusedLabelColor = Theme.colors.textSecondary,
        ),
        shape = RoundedCornerShape(8.dp),
        label = {
            Text(
                text = "ММ:СС.МС",
                color = Theme.colors.support20
            )
        },
        value = textFieldValue,
        onValueChange = { newValue ->
            val newTextRaw = newValue.text
            val digitsOnly = newTextRaw.filter { it.isDigit() }
            if (digitsOnly.length <= 6) {
                val formatted = formatTime(digitsOnly)
                val cursorPosition = calculateCursorPosition(
                    newTextRaw,
                    formatted,
                    newValue.selection.start
                )

                viewModel.onInputChanged(
                    TextFieldValue(
                        text = formatted,
                        selection = TextRange(cursorPosition)
                    )
                )
            }
        },
        trailingIcon = {
            IconButton(
                content = {
                    Icon(
                        painterResource(Res.drawable.swim_results_close),
                        null,
                        tint = Theme.colors.textPrimary,
                        modifier = Modifier.width(16.dp).height(16.dp)
                    )
                },
                onClick = { viewModel.onInputChanged(TextFieldValue()) },
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

fun formatTime(digits: String): String {
    return when (digits.length) {
        0 -> ""
        1, 2 -> digits // Treated as ms
        3 -> {
            // First digit is Seconds, last two are ms
            "${digits[0]}.${digits.substring(1)}"
        }

        4 -> {
            // First two are Seconds, last two are ms
            "${digits.take(2)}.${digits.substring(2)}"
        }

        5 -> {
            // First digit is Min, middle two Sec, last two ms
            "${digits[0]}:${digits.substring(1, 3)}.${digits.substring(3)}"
        }

        6 -> {
            // Standard MM.SS.mm
            "${digits.take(2)}:${digits.substring(2, 4)}.${digits.substring(4)}"
        }

        else -> digits
    }
}

private fun calculateCursorPosition(rawInput: String, formatted: String, rawCursor: Int): Int {
    if (rawCursor == 0) return 0

    // Count how many digits were before the cursor in the unformatted input
    val digitsBeforeCursor = rawInput.take(rawCursor).count { it.isDigit() }

    // Find the position in the formatted string that contains that many digits
    var currentDigits = 0
    formatted.forEachIndexed { index, char ->
        if (char.isDigit()) currentDigits++
        if (currentDigits == digitsBeforeCursor) {
            return index + 1
        }
    }
    return formatted.length
}