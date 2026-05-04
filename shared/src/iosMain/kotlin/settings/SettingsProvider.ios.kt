package settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

actual object SettingsProvider {
    actual fun get(): Settings {
        return NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) // TODO secure
    }
}