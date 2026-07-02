package i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key

@Composable
actual fun ProvideAppLocale(
    langTag: String?,
    content: @Composable (() -> Unit)
) {
    key(langTag) { content() }
}