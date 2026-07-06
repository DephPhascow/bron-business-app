package settings

interface AuthPref {
    var accessToken: String?
    var refreshToken: String?
    var accessTokenUpdatedAt: Long?
}
