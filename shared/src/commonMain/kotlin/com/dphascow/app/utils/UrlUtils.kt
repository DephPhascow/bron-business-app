package com.dphascow.app.utils

/**
 * Resolves an image/file path returned by the API into a full URL.
 * Mirrors the client app: absolute URLs pass through, relative paths get [apiHost] prepended.
 */
fun resolveFullUrl(apiHost: String, path: String?): String? {
    if (path == null) return null
    val p = path.trim()
    if (p.isEmpty()) return null
    if (p.startsWith("http://") || p.startsWith("https://")) return p
    val host = apiHost.trimEnd('/')
    return when {
        p.startsWith("/") -> host + p
        p.startsWith(host) -> p
        else -> "$host/$p"
    }
}
