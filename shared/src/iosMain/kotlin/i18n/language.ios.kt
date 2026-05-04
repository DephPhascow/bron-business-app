package i18n

import androidx.compose.runtime.Composable

@Composable
actual fun ProvideAppLocale(
    langTag: String?,
    content: @Composable (() -> Unit)
) {
    content()
}