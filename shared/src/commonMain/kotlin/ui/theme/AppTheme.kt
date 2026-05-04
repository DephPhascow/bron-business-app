package ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import settings.ThemeMode


@Composable
fun AppTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit
) {
    // Подобие system, но единообразно: решаем до MaterialTheme
    val isDark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> false // если нужен реальный системный флаг — прокинь с платформы
    }

    val appColors = remember(isDark) { if (isDark) darkColors() else lightColors() }
    val appTypography = remember { baseTypography() }
    val dimens = remember { AppDimens() }

    val m3 = if (isDark) {
        darkColorScheme(
            primary = appColors.primary,
            background = appColors.background,
            surface = appColors.surface,
            error = appColors.error
        )
    } else {
        lightColorScheme(
            primary = appColors.primary,
            background = appColors.background,
            surface = appColors.surface,
            error = appColors.error
        )
    }

    CompositionLocalProvider(
        LocalColors provides appColors,
        LocalTypography provides appTypography,
        LocalDimens provides dimens
    ) {
        MaterialTheme(
            colorScheme = m3,
            typography = androidx.compose.material3.Typography(
                displayLarge = appTypography.h1,
                displayMedium = appTypography.h1,
                bodyLarge = TextStyle(fontSize = appTypography.body.sp, fontFamily = appTypography.fontFamily),
                bodySmall = TextStyle(fontSize = appTypography.caption.sp, fontFamily = appTypography.fontFamily),
            ),
            content = content
        )
    }
}

// Удобные getters
object T {
    val c @Composable get() = LocalColors.current
    val t @Composable get() = LocalTypography.current
    val d @Composable get() = LocalDimens.current
}
