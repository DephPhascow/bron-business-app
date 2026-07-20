package settings

interface AuthPref {
    var accessToken: String?
    var refreshToken: String?
    var accessTokenUpdatedAt: Long?

    /**
     * Stable per-installation UUID sent as `X-Device-Id`. The backend binds access
     * tokens to it, so it is generated once and must survive log out — losing it
     * invalidates every token this install holds.
     */
    val deviceId: String
}
