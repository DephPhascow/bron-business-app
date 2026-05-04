package settings

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

lateinit var appContext: Context // установи в Application.onCreate

actual object SettingsProvider {
    actual fun get(): Settings =
        SharedPreferencesSettings(appContext.getSharedPreferences("prefs", 0))
}
