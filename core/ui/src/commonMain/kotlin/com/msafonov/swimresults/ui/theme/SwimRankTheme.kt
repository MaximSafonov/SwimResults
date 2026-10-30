package com.msafonov.swimresults.ui.theme

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
    val inverseColors = if (isDark) SwimRankLightColorScheme else SwimRankDarkColorScheme
    val materialColors = (if (isDark) darkColorScheme() else lightColorScheme()).copy(
        primary = colors.primary,
        onPrimary = colors.textContrast,
        primaryContainer = colors.primary,
        onPrimaryContainer = colors.textContrast,
        inversePrimary = inverseColors.primary,
        secondary = colors.primary,
        onSecondary = colors.textContrast,
        secondaryContainer = colors.backGround2,
        onSecondaryContainer = colors.textPrimary,
        tertiary = colors.primary,
        onTertiary = colors.textContrast,
        tertiaryContainer = colors.backGround2,
        onTertiaryContainer = colors.textPrimary,
        background = colors.backGround,
        onBackground = colors.textPrimary,
        surface = colors.backGround,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.backGround2,
        onSurfaceVariant = colors.textSecondary,
        surfaceTint = Color.Transparent,
        inverseSurface = inverseColors.backGround2,
        inverseOnSurface = inverseColors.textPrimary,
        error = colors.errorPrimary,
        onError = if (isDark) Red10 else colors.textContrast,
        errorContainer = if (isDark) Red30 else Red90,
        onErrorContainer = if (isDark) Red90 else Red10,
        outline = colors.textSecondary,
        outlineVariant = colors.support20,
        scrim = Color.Black,
        surfaceBright = colors.backGround,
        surfaceDim = colors.backGround2,
        surfaceContainerLowest = colors.backGround2,
        surfaceContainerLow = colors.backGround,
        surfaceContainer = colors.backGround2,
        surfaceContainerHigh = colors.backGround2,
        surfaceContainerHighest = colors.backGround2,
        primaryFixed = Ocean2,
        primaryFixedDim = Ocean4,
        onPrimaryFixed = TextPrimaryLight,
        onPrimaryFixedVariant = PrimaryLight,
        secondaryFixed = Ocean2,
        secondaryFixedDim = Ocean4,
        onSecondaryFixed = TextPrimaryLight,
        onSecondaryFixedVariant = PrimaryLight,
        tertiaryFixed = Ocean2,
        tertiaryFixedDim = Ocean4,
        onTertiaryFixed = TextPrimaryLight,
        onTertiaryFixedVariant = PrimaryLight
    )

    CompositionLocalProvider(
        SwimRankColors provides colors,
        LocalRippleConfiguration provides customRipple,
        LocalContentColor provides colors.textPrimary
    ) {
        MaterialTheme(colorScheme = materialColors, shapes = SwimRankShapes, content = content)
    }

}

object Theme {
    val colors: ColorPalette
        @Composable
        get() = SwimRankColors.current

    val shapes: Shapes
        @Composable
        get() = MaterialTheme.shapes
}
