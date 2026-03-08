package com.msafonov.swimresults.ui.theme

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val SwimRankDarkColorScheme = ColorPaletteDark()
val SwimRankLightColorScheme = ColorPaletteLight()


@OptIn(ExperimentalMaterial3Api::class)
val customRipple = RippleConfiguration(
    color = Color.Transparent,
    rippleAlpha = RippleAlpha(0.2f, 0.2f, 0.1f, 0.2f)
)

val SwimRankColors = staticCompositionLocalOf<ColorPalette> {
    error("No colors provided")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwimRankAppTheme(
    isDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (isDark) SwimRankDarkColorScheme else SwimRankLightColorScheme

    CompositionLocalProvider(
        SwimRankColors provides colors,
        LocalRippleConfiguration provides customRipple
    ) {
        content()
    }

}

object Theme {
    val colors: ColorPalette
        @Composable
        get() = SwimRankColors.current
}
