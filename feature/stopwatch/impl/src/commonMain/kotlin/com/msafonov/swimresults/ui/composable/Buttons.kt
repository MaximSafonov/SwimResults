package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.msafonov.swimresults.ui.theme.SwimComponentDefaults

@Composable
fun ButtonPrimary(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        colors = SwimComponentDefaults.primaryButtonColors(),
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        onClick = { onClick.invoke() }) {
        Text(text = text)
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
        colors = SwimComponentDefaults.secondaryButtonColors(),
        modifier = modifier.fillMaxWidth().height(56.dp),
        enabled = enabled,
        onClick = { onClick.invoke() }) {
        Text(text = text)
    }
}
