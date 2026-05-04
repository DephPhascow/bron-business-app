package settings

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual object SettingsProvider {
    actual fun get(): Settings =
        PreferencesSettings(Preferences.userRoot().node("app_prefs"))
}
