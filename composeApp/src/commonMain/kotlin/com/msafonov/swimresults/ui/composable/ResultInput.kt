package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.msafonov.swimresults.ui.theme.Theme

@Composable
fun ResultUnput(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    value: String,
    onValueChanged: (millis: String) -> Unit
) {
    val maxLength = 2

    OutlinedTextField(
        modifier = modifier,
        value = value,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Theme.colors.textPrimary,
            unfocusedTextColor = Theme.colors.surface3,
            cursorColor = Theme.colors.surface3,
            focusedContainerColor = Theme.colors.surface2,
            unfocusedContainerColor = Theme.colors.surface1,
            focusedLabelColor = Theme.colors.primary,
            unfocusedLabelColor = Theme.colors.textSecondary,
            focusedBorderColor = Theme.colors.primary,
            unfocusedBorderColor = Theme.colors.primary
        ),
        onValueChange = { millis ->
            if (millis.length <= maxLength) {
                onValueChanged(millis)
            }
        },
        label = label,
        textStyle = TextStyle(
            color = Theme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

