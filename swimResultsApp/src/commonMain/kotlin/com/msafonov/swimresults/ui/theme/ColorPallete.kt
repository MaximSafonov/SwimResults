package com.msafonov.swimresults.ui.theme

import androidx.compose.ui.graphics.Color

interface ColorPalette {
    val primary: Color
    val onPrimary: Color
    val success: Color
    val attention: Color
    val errorPrimary: Color
    val textPrimary: Color
    val textSecondary: Color
    val textContrast: Color
    val support60: Color
    val support20: Color
    val backGround: Color
    val backGround2: Color
    val surface1: Color
    val surface2: Color
    val surface3: Color
}

data class ColorPaletteDark(
    override val primary: Color = PrimaryDark,
    override val onPrimary: Color = onPrimaryDark,
    override val success: Color = SuccessDark,
    override val attention: Color = AttentionDark,
    override val errorPrimary: Color = ErrorPrimaryDark,
    override val textPrimary: Color = TextPrimaryDark,
    override val textSecondary: Color = TextSecondaryDark,
    override val textContrast: Color = TextContrastDark,
    override val support60: Color = Support60Dark,
    override val support20: Color = Support20Dark,
    override val backGround: Color = BackGroundDark,
    override val backGround2: Color = BackGround2Dark,
    override val surface1: Color = Surface1Dark,
    override val surface2: Color = Surface2Dark,
    override val surface3: Color = Surface3Dark,
) : ColorPalette

data class ColorPaletteLight(
    override val primary: Color = PrimaryLight,
    override val onPrimary: Color = onPrimaryLight,
    override val success: Color = SuccessLight,
    override val attention: Color = AttentionLight,
    override val errorPrimary: Color = ErrorPrimaryLight,
    override val textPrimary: Color = TextPrimaryLight,
    override val textSecondary: Color = TextSecondaryLight,
    override val textContrast: Color = TextContrastLight,
    override val support60: Color = Support60Light,
    override val support20: Color = Support20Light,
    override val backGround: Color = BackGroundLight,
    override val backGround2: Color = BackGround2Light,
    override val surface1: Color = Surface1Light,
    override val surface2: Color = Surface2Light,
    override val surface3: Color = Surface3Light,
) : ColorPalette

// Base colors
val PrimaryDark = Color(0xff006d9e)
val PrimaryLight = Color(0xff005687)

val onPrimaryDark = Color(0xff13d0e4)
val onPrimaryLight = Color(0xff57eff5)

val SuccessDark = Color(0xff52c41a)
val SuccessLight = Color(0xff52c41a)


val AttentionDark = Color(0xFFFABD1B)
val AttentionLight = Color(0xFFFABD1B)


val ErrorPrimaryDark = Color(0xFF930006)
val ErrorPrimaryLight = Color(0xFF930006)

val TextPrimaryDark = Color(0xfff2ffff)
val TextPrimaryLight = Color(0xFF002F36)

val TextSecondaryDark = Color(0x99FFFFFF)
val TextSecondaryLight = Color(0x991C232D)

val TextContrastDark = Color(0xfff2ffff)
val TextContrastLight = Color(0xfff2ffff)

val BackGroundDark = Color(0xFF1C232D)
val BackGroundLight = Color(0xFFECECEC)

val BackGround2Dark = Color(0xFF12161D)
val BackGround2Light = Color(0xFFD8D8D8)

val Surface1Dark = Color(0xff005687)
val Surface1Light = Color(0xff00a1c7)

val Surface2Dark = Color(0xff0087b3)
val Surface2Light =   Color(0xff00b9d7)

val Surface3Dark = Color(0xff00b9d7)
val Surface3Light = Color(0xff0087b3)

val Support60Dark = Color(0x99EBEBF5)
val Support60Light = Color(0x99333537)

val Support20Dark = Color(0x33FFFFFF)
val Support20Light = Color(0x22000000)


val Ocean3 = Color(0xff86f7fa)
val Ocean11 = Color(0xff005687)



val Ocean10 = Color(0xff006d9e)
val Ocean9 = Color(0xff0087b3)
val Ocean8 = Color(0xff00a1c7)
val Ocean7 = Color(0xff00b9d7)
val Ocean6 = Color(0xff13d0e4)
val Ocean5 = Color(0xff30e2ed)
val Ocean4 = Color(0xff57eff5)

val Ocean2 = Color(0xffbbfdfd)
val Ocean1 = Color(0xffd6fefe)
val Ocean0 = Color(0xfff2ffff)

