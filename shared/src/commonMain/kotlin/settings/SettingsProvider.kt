// settings/SettingsProvider.kt
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package settings

import com.russhwolf.settings.Settings

expect object SettingsProvider {
    fun get(): Settings
}
