package com.msafonov.swimresults.ui.theme

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SwimRankDarkColorScheme = darkColorScheme(
    primary = Ocean10,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    inversePrimary = Blue40,
    secondary = DarkBlue80,
    onSecondary = DarkBlue20,
    secondaryContainer = DarkBlue30,
    onSecondaryContainer = DarkBlue90,
    tertiary = Yellow80,
    onTertiary = Yellow20,
    tertiaryContainer = Yellow30,
    onTertiaryContainer = Yellow90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Ocean4,
    onBackground = Grey90,
    surface = Ocean4,
    onSurface = Ocean0,
    inverseSurface = Grey90,
    inverseOnSurface = Grey20,
    surfaceVariant = BlueGrey30,
    onSurfaceVariant = Ocean1,
    outline = Ocean5,
    surfaceContainerLowest = Blue10
)

val SwimRankLightColorScheme = lightColorScheme(
    primary = Ocean10,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    inversePrimary = Blue80,
    secondary = DarkBlue40,
    onSecondary = Color.White,
    secondaryContainer = DarkBlue90,
    onSecondaryContainer = DarkBlue10,
    tertiary = Yellow40,
    onTertiary = Color.White,
    tertiaryContainer = Yellow90,
    onTertiaryContainer = Yellow10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Ocean5,
    onBackground = Grey10,
    surface = Ocean5,
    onSurface = Grey10,
    inverseSurface = Grey20,
    inverseOnSurface = Grey95,
    surfaceVariant = BlueGrey90,
    onSurfaceVariant = BlueGrey30,
    outline = BlueGrey50,
    surfaceContainerLowest = Grey99,

)


@Composable
fun SwimRankTheme(isDarkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDarkTheme) SwimRankDarkColorScheme else SwimRankLightColorScheme,
        content = content,
    )
}


val chipColors = SelectableChipColors(
    selectedLabelColor = FunctionalGrey,
    containerColor = Ocean7,
    labelColor = FunctionalGrey,
    leadingIconColor = DarkBlue90,
    trailingIconColor = DarkBlue90,
    disabledContainerColor = Ocean7,
    disabledLabelColor = FunctionalGrey,
    disabledLeadingIconColor = Ocean3,
    disabledTrailingIconColor = Ocean3,
    selectedContainerColor = Ocean10,
    disabledSelectedContainerColor = Ocean3,
    selectedLeadingIconColor = Ocean3,
    selectedTrailingIconColor = Ocean3
)

val primaryButtonColors = ButtonColors(
    containerColor = Ocean8,
    disabledContainerColor = Ocean7,
    contentColor = FunctionalGrey,
    disabledContentColor = FunctionalGrey,
)

val secondaryButtonColors = ButtonColors(
    containerColor = Ocean10,
    disabledContainerColor = Ocean8,
    contentColor = FunctionalGrey,
    disabledContentColor = FunctionalGrey,
)

val radioColors = RadioButtonColors(
    selectedColor = Ocean10,
    unselectedColor = Ocean4,
    disabledSelectedColor = Ocean8,
    disabledUnselectedColor= Ocean2

)

val textFieldColors = TextFieldColors(
    cursorColor = Ocean4,
    focusedTextColor = Ocean4,
    focusedContainerColor = Ocean10,
    unfocusedContainerColor = Ocean10,
    disabledContainerColor = Ocean10,
    focusedLabelColor = Ocean4,
    unfocusedLabelColor = Ocean4,
    disabledLabelColor = Ocean4,
    focusedSupportingTextColor = Ocean4,
    unfocusedSupportingTextColor = Ocean4,
    disabledSupportingTextColor = Ocean4,
    errorSupportingTextColor = Ocean4,
    unfocusedTextColor = Ocean4,
    disabledTextColor = Ocean4,
    errorTextColor = Ocean4,
    errorContainerColor = Ocean4,
    errorCursorColor = Ocean4,

    focusedIndicatorColor = Ocean4,
    unfocusedIndicatorColor = Ocean4,
    disabledIndicatorColor = Ocean4,
    errorIndicatorColor = Ocean4,
    focusedLeadingIconColor = Ocean4,
    unfocusedLeadingIconColor = Ocean4,
    disabledLeadingIconColor = Ocean4,
    errorLeadingIconColor = Ocean4,
    focusedTrailingIconColor = Ocean4,
    unfocusedTrailingIconColor = Ocean4,
    disabledTrailingIconColor = Ocean4,
    errorTrailingIconColor = Ocean4,
    errorLabelColor = Ocean4,
    focusedPlaceholderColor = Ocean4,
    unfocusedPlaceholderColor = Ocean4,
    disabledPlaceholderColor = Ocean4,
    errorPlaceholderColor = Ocean4,
    focusedPrefixColor = Ocean4,
    unfocusedPrefixColor = Ocean4,
    disabledPrefixColor = Ocean4,
    errorPrefixColor = Ocean4,
    focusedSuffixColor = Ocean4,
    unfocusedSuffixColor = Ocean4,
    disabledSuffixColor = Ocean4,
    errorSuffixColor = Ocean4,
    textSelectionColors = TextSelectionColors(
        Ocean4, Ocean10
    ),
)