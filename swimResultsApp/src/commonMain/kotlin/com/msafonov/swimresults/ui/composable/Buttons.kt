package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.msafonov.swimresults.ui.theme.Theme

@Composable
fun ButtonPrimary(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Theme.colors.primary,
            disabledContainerColor = Theme.colors.primary.copy(alpha = 0.5f),
        ),
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        onClick = { onClick.invoke() }) {
        Text(
            text = text,
            color = Theme.colors.onPrimary
        )
    }
}

@Composable
fun ButtonSecondary(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Theme.colors.surface3,
            disabledContainerColor = Theme.colors.support60,
        ),
        modifier = modifier.fillMaxWidth().height(56.dp),
        enabled = enabled,
        onClick = { onClick.invoke() }) {
        Text(
            text = text,
            color = Theme.colors.textPrimary
        )
    }
}