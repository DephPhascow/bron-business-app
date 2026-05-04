package ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val error: Color,
    val isDark: Boolean,
    val borderColor: Color,
    val dark1: Color,
    val dark3: Color,
    val dark4: Color,
    val dark5: Color,
    val dark7: Color,
    val dark10: Color,
    val graniteGreen7: Color,
    val copper5: Color,
    val redError: Color,
    val yellow8: Color,
    val metallicCopper7: Color,
)

data class AppTypography(
    val h1: TextStyle,   // sp
    val h2: TextStyle,
    val h3: TextStyle,
    val body: Float,
    val caption: Float,
    val fontFamily: FontFamily,

    val t1: TextStyle,
    val t2: TextStyle,
    val t2Regular: TextStyle,
    val t2Bold: TextStyle,
    val t3: TextStyle,
    val t3SemiBold: TextStyle,
    val t4: TextStyle,
    val t5: TextStyle,
    var t12: TextStyle,
    var t14: TextStyle,
    val t4Bold: TextStyle,
    val t4SamiBold: TextStyle,
    val b14: TextStyle,
    val headingH3: TextStyle,
)

data class AppDimens(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,

    val paddingMain: Dp = 16.dp
)

val LocalColors = staticCompositionLocalOf<AppColors> { error("No AppColors") }
val LocalTypography = staticCompositionLocalOf<AppTypography> { error("No AppTypography") }
val LocalDimens = staticCompositionLocalOf { AppDimens() }

// Пресеты тем
fun lightColors() = AppColors(
    primary = Color(0xFF0F0F0F),
    onPrimary = Color.White,
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF222222),
    error = Color(0xFFEB5757),
    borderColor = Color(0xFF949494),
    isDark = false,
    dark1 = Color(0xFFFFFFFF),
    dark4 = Color(0xFFAFAFAF),
    dark3 = Color(0xFFC9C9C9),
    dark5 = Color(0xFF949494),
    dark7 = Color(0xFF5F5F5F),
    dark10 = Color(0xFF0F0F0F),
    graniteGreen7 = Color(0xFF898970),
    copper5 = Color(0xFF8F493D),
    redError = Color(0xFFF70C18),
    yellow8 = Color(0xFFFFA42D),
    metallicCopper7 = Color(0xFF722D1E),
)

fun darkColors() = AppColors(
    primary = Color(0xFFFFFFFF),        // основной акцент светлый
    onPrimary = Color(0xFF0F0F0F),      // текст/иконки на primary
    background = Color(0xFF111111),     // фон
    onBackground = Color(0xFFF7F8FA),   // текст на фоне
    surface = Color(0xFF222222),        // карточки, элементы UI
    onSurface = Color(0xFFFFFFFF),      // текст/иконки на surface
    error = Color(0xFFEB5757),          // ошибки обычно одинаковые
    borderColor = Color(0xFF5F5F5F),    // границы светлее на тёмном фоне
    isDark = true,

    // «инвертированные» тона
    dark1 = Color(0xFF0F0F0F),          // самый тёмный
    dark5 = Color(0xFF5F5F5F),          // средний серый
    dark3 = Color(0xFFC9C9C9),
    dark4 = Color(0xFFAFAFAF),
    dark7 = Color(0xFF949494),          // светлее серый
    dark10 = Color(0xFFFFFFFF),         // белый для текста
    graniteGreen7 = Color(0xFF6A6A6A),   // чуть мягче на тёмном фоне
    copper5 = Color(0xFF8F493D),
    redError = Color(0xFFF70C18),
    yellow8 = Color(0xFFFFA42D),
    metallicCopper7 = Color(0xFF722D1E),
)




fun baseTypography() = AppTypography(
    h1 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 34.sp
    ),
    h2 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 26.sp
    ),
    h3 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 22.sp
    ),
    body = 16f,
    caption = 12f,
    fontFamily = FontFamily.Default,
    t1 = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 18.sp,
    ),
    t2 = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
    ),
    t2Regular = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
    ),
    t2Bold = TextStyle(
        fontWeight = FontWeight.W700,
        fontSize = 16.sp,
    ),
    t3 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
    ),
    t3SemiBold = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
    ),
    t4Bold = TextStyle(
        fontWeight = FontWeight.W700,
        fontSize = 12.sp,
    ),
    t4SamiBold = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
    ),
    t4 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
    ),
    t5 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 11.sp,
    ),
    t12 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
    ),
    t14 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
    ),
    b14 = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
    ),
    headingH3 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 22.sp,
    ),
)
