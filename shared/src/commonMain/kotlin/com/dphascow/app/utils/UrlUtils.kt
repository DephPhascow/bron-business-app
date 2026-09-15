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

private val quotedUrlField = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"")
private val quotedKeyField = Regex("\"key\"\\s*:\\s*\"([^\"]+)\"")

/**
 * Chat attachments sent by older builds of the client app stored the whole upload
 * response in `fileUrl` instead of its `key`, so a raw value can be a path, an
 * absolute url, or a JSON object. Pulls the path back out of all three.
 */
fun extractAttachmentPath(raw: String?): String? {
    val trimmed = raw?.trim()?.removeSurrounding("\"")?.trim()
    if (trimmed.isNullOrEmpty()) return null

    if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("/")) {
        return trimmed
    }

    val decoded = trimmed.replace("\\/", "/").replace("\\\"", "\"")
    quotedUrlField.find(decoded)?.groupValues?.get(1)?.let { return it.replace("\\/", "/").trim() }
    quotedKeyField.find(decoded)?.groupValues?.get(1)?.let { return it.replace("\\/", "/").trim() }
    return decoded
}
