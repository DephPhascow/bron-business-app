package i18n

import androidx.compose.runtime.Composable

@Composable
expect fun ProvideAppLocale(
    langTag: String?,                      // "ru", "uz", "ru-RU" или null (системный)
    content: @Composable () -> Unit
)