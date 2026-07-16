package settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class Prefs(
    private val s: com.russhwolf.settings.Settings = SettingsProvider.get()
) : AuthPref {
    private val _lang = MutableStateFlow(s.getStringOrNull("lang"))

    var lang: String?            // "ru" / "uz" / null (= системный)
        get() = _lang.value
        set(v) {
            if (v == null) s.remove("lang") else s.putString("lang", v)
            _lang.value = v
        }

    private val _theme = MutableStateFlow(runCatching {
        ThemeMode.valueOf(s.getString("theme", ThemeMode.SYSTEM.name))
    }.getOrDefault(ThemeMode.SYSTEM))

    var theme: ThemeMode
        get() = _theme.value
        set(v) {
            s.putString("theme", v.name)
            _theme.value = v
        }

    // TODO затем сделать защищенное хранение
    private val _accessToken = MutableStateFlow(s.getStringOrNull("accessToken"))
    val accessTokenFlow: StateFlow<String?> = _accessToken

    private val _refreshToken = MutableStateFlow(s.getStringOrNull("refreshToken"))
    val refreshTokenFlow: StateFlow<String?> = _refreshToken

    private val _selectedBusinessId = MutableStateFlow(s.getStringOrNull("selectedBusinessId"))
    val selectedBusinessIdFlow: StateFlow<String?> = _selectedBusinessId

    private val _selectedBusinessName = MutableStateFlow(s.getStringOrNull("selectedBusinessName"))
    val selectedBusinessNameFlow: StateFlow<String?> = _selectedBusinessName

    private val _selectedBusinessRole = MutableStateFlow(s.getStringOrNull("selectedBusinessRole"))
    val selectedBusinessRoleFlow: StateFlow<String?> = _selectedBusinessRole

    private val _lastLoginPhone = MutableStateFlow(s.getStringOrNull("lastLoginPhone"))
    val lastLoginPhoneFlow: StateFlow<String?> = _lastLoginPhone

    private val _rememberBusinessSelection = MutableStateFlow(s.getBoolean("rememberBusinessSelection", false))
    val rememberBusinessSelectionFlow: StateFlow<Boolean> = _rememberBusinessSelection

    private val _rememberedBusinessId = MutableStateFlow(s.getStringOrNull("rememberedBusinessId"))
    val rememberedBusinessIdFlow: StateFlow<String?> = _rememberedBusinessId

    override var accessToken: String?
        get() = _accessToken.value
        set(v) {
            if (v == null) s.remove("accessToken") else s.putString("accessToken", v)
            _accessToken.value = v
            accessTokenUpdatedAt = if (v != null) currentTimeMillis() else null
        }

    override var accessTokenUpdatedAt: Long?
        get() = if (s.hasKey("accessTokenUpdatedAt")) s.getLong("accessTokenUpdatedAt", 0L) else null
        set(v) {
            if (v == null) s.remove("accessTokenUpdatedAt") else s.putLong("accessTokenUpdatedAt", v)
        }

    override var refreshToken: String?
        get() = _refreshToken.value
        set(v) {
            if (v == null) s.remove("refreshToken") else s.putString("refreshToken", v)
            _refreshToken.value = v
        }

    var selectedBusinessId: String?
        get() = _selectedBusinessId.value
        set(v) {
            if (v == null) s.remove("selectedBusinessId") else s.putString("selectedBusinessId", v)
            _selectedBusinessId.value = v
        }

    var selectedBusinessName: String?
        get() = _selectedBusinessName.value
        set(v) {
            if (v == null) s.remove("selectedBusinessName") else s.putString("selectedBusinessName", v)
            _selectedBusinessName.value = v
        }

    var selectedBusinessRole: String?
        get() = _selectedBusinessRole.value
        set(v) {
            if (v == null) s.remove("selectedBusinessRole") else s.putString("selectedBusinessRole", v)
            _selectedBusinessRole.value = v
        }

    var lastLoginPhone: String?
        get() = _lastLoginPhone.value
        set(v) {
            if (v == null) s.remove("lastLoginPhone") else s.putString("lastLoginPhone", v)
            _lastLoginPhone.value = v
        }

    var rememberBusinessSelection: Boolean
        get() = _rememberBusinessSelection.value
        set(v) {
            s.putBoolean("rememberBusinessSelection", v)
            _rememberBusinessSelection.value = v
        }

    var rememberedBusinessId: String?
        get() = _rememberedBusinessId.value
        set(v) {
            if (v == null) s.remove("rememberedBusinessId") else s.putString("rememberedBusinessId", v)
            _rememberedBusinessId.value = v
        }

    fun clearRememberedBusiness() {
        rememberedBusinessId = null
    }

    fun clearSelectedBusiness() {
        selectedBusinessId = null
        selectedBusinessName = null
        selectedBusinessRole = null
    }

    fun clearAuthSession() {
        accessToken = null
        refreshToken = null
        clearSelectedBusiness()
    }
}
