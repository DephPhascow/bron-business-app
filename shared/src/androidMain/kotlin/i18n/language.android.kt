package i18n

import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
actual fun ProvideAppLocale(langTag: String?, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val resources = ctx.resources
    val config = resources.configuration

    if (langTag != null) {
        val locale = Locale.forLanguageTag(langTag)
        Locale.setDefault(locale)
        config.setLocales(LocaleList(locale))
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    } else {
        // вернуться к системной
        val sys = config.locales.get(0)
        Locale.setDefault(sys)
    }

    // форсим перерисовку при смене языка
    key(langTag) { content() }
}