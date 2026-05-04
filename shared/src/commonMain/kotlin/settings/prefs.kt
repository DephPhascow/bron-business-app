package settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class Prefs(
    private val s: com.russhwolf.settings.Settings = SettingsProvider.get()
) {
    var lang: String?            // "ru" / "uz" / null (= системный)
        get() = s.getStringOrNull("lang")
        set(v) = if (v == null) s.remove("lang") else s.putString("lang", v)

    var theme: ThemeMode
        get() = s.getString("theme", ThemeMode.SYSTEM.name).let { ThemeMode.valueOf(it) }
        set(v) = s.putString("theme", v.name)

    // TODO временно, затем сделать защищенное хранение
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessTokenFlow: StateFlow<String?> = _accessToken

    private val _refreshToken = MutableStateFlow<String?>(null)
    val refreshTokenFlow: StateFlow<String?> = _refreshToken

    var accessToken: String?
        get() = _accessToken.value
        set(v) { _accessToken.value = v }

    var refreshToken: String?
        get() = _refreshToken.value
        set(v) { _refreshToken.value = v }
}
