package com.msafonov.swimresults.ui.composable

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msafonov.swimresults.ui.theme.Blue30

@Composable
fun ResultUnput(label: String, value: String, onValueChanged: (millis: String) -> Unit) {
    val brush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Blue,
                Color.Magenta
            )
        )
    }
    val maxLength = 2

    OutlinedTextField(
        value = value,
        onValueChange = { millis ->
            if (millis.length <= maxLength) {
                onValueChanged(millis)
            }

        },
        label = ({ Text(label) }),
        textStyle = TextStyle(
            color = Blue30,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        ),
        modifier = Modifier.size(height = 70.dp, width = 120.dp).padding(2.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}