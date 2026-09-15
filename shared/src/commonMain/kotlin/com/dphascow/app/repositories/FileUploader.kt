package com.dphascow.app.repositories

import com.dphascow.BuildKonfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

/** What `/api/files/upload` gives back: [key] is what we persist, [url] is what we show. */
data class UploadedFile(
    val key: String,
    val url: String,
)

/** Raised when the server rejects the file itself (wrong type, too large). */
class FileUploadException(message: String) : Exception(message)

/**
 * Uploads raw file bytes to the REST `/files/upload` endpoint (multipart) and
 * returns the stored file, which is then persisted via GraphQL
 * (`business.logoUrl`, gallery image, …).
 *
 * The endpoint requires authentication, so this shares the app's [TokenProvider]
 * rather than posting anonymously.
 */
class FileUploader(
    private val tokenProvider: TokenProvider,
    private val deviceId: String,
) {
    /**
     * Convenience for the callers that only need somewhere to point an `imageUrl` at.
     *
     * Returns the [UploadedFile.key] — the host-relative path — because that is what
     * the backend asks clients to store; the absolute `url` it also returns is built
     * from the request host, so persisting it would freeze today's domain into the
     * database. Everything that renders these goes through `resolveFullUrl`, which
     * takes both.
     */
    suspend fun uploadImage(bytes: ByteArray, fileName: String?, mimeType: String?): String =
        upload(bytes, fileName, mimeType).key

    suspend fun upload(bytes: ByteArray, fileName: String?, mimeType: String?): UploadedFile {
        if (bytes.size > MAX_UPLOAD_BYTES) {
            throw FileUploadException("File is larger than ${MAX_UPLOAD_BYTES / BYTES_IN_MB} MB")
        }

        // The server cross-checks the extension, the Content-Type and the file's own
        // leading bytes, and answers 415 when they disagree. The photo picker can hand
        // us a name with no extension at all, so derive one type from the content and
        // send a name, an extension and a Content-Type that all agree with it.
        val extension = resolveExtension(bytes, fileName, mimeType)
        val contentType = MIME_BY_EXTENSION.getValue(extension)
        val name = safeFileName(fileName, extension)

        val client = HttpClient {
            // Photos go up over mobile networks; without this a stalled connection
            // leaves the upload spinning forever instead of failing.
            install(HttpTimeout) { requestTimeoutMillis = UPLOAD_TIMEOUT_MS }
        }
        try {
            var response = client.post(bytes, name, contentType, requireToken(forceRefresh = false))
            // Access tokens live 5 minutes. GraphQL calls refresh and retry on their
            // own, and uploads have to do the same or a stale token surfaces to the
            // user as "not authenticated" mid-session.
            if (response.status.value == 401) {
                response = client.post(bytes, name, contentType, requireToken(forceRefresh = true))
            }

            val text = response.bodyAsText()
            when (response.status.value) {
                401 -> throw IllegalStateException("Not authenticated")
                413 -> throw FileUploadException("File is too large (max ${MAX_UPLOAD_BYTES / BYTES_IN_MB} MB)")
                415 -> throw FileUploadException("Unsupported file type: ${ALLOWED_EXTENSIONS.joinToString(", ")}")
                429 -> throw RateLimitException()
            }

            val url = jsonField("url").find(text)?.groupValues?.get(1)
                ?: throw FileUploadException("Upload failed: $text")
            // `key` is what the backend wants stored; fall back to the url so an older
            // server that omits it still works.
            val key = jsonField("key").find(text)?.groupValues?.get(1) ?: url

            return UploadedFile(key = key, url = url)
        } finally {
            client.close()
        }
    }

    private suspend fun requireToken(forceRefresh: Boolean): String =
        tokenProvider.accessToken(forceRefresh) ?: throw IllegalStateException("Not authenticated")

    private suspend fun HttpClient.post(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
        token: String,
    ): HttpResponse = submitFormWithBinaryData(
        url = "${BuildKonfig.API_API}/files/upload",
        formData = formData {
            append(
                key = "file",
                value = bytes,
                headers = Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    append(HttpHeaders.ContentType, contentType)
                },
            )
        },
    ) {
        header(HttpHeaders.Authorization, "Bearer $token")
        header(DEVICE_ID_HEADER, deviceId)
        // Must match the agent Apollo sends: the server hashes it into the token
        // fingerprint, so a different value here reads as a different device.
        header(HttpHeaders.UserAgent, USER_AGENT)
    }

    private companion object {
        const val BYTES_IN_MB = 1024 * 1024
        const val MAX_UPLOAD_BYTES = 20 * BYTES_IN_MB
        const val UPLOAD_TIMEOUT_MS = 60_000L

        /** Exactly what the backend whitelists, each with the type it expects alongside. */
        val MIME_BY_EXTENSION = mapOf(
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "png" to "image/png",
            "webp" to "image/webp",
            "gif" to "image/gif",
            "pdf" to "application/pdf",
        )
        val EXTENSION_BY_MIME = mapOf(
            "image/jpeg" to "jpg",
            "image/jpg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
            "image/gif" to "gif",
            "application/pdf" to "pdf",
        )
        val ALLOWED_EXTENSIONS = MIME_BY_EXTENSION.keys.toList()

        /** The response is a flat JSON object, so a field-level regex is enough. */
        fun jsonField(name: String) = Regex("\"$name\"\\s*:\\s*\"([^\"]+)\"")

        /**
         * The file's own signature wins: the picker's MIME type and the file name are
         * both hints (a screenshot renamed to `.jpg` is still a PNG) while the server
         * goes by the bytes. HEIC and friends match nothing here and are refused
         * locally instead of costing a round trip and a 415.
         */
        fun resolveExtension(bytes: ByteArray, fileName: String?, mimeType: String?): String =
            sniffExtension(bytes)
                ?: EXTENSION_BY_MIME[mimeType?.substringBefore(';')?.trim()?.lowercase()]
                ?: fileName?.substringAfterLast('.', "")?.lowercase()?.takeIf { it in MIME_BY_EXTENSION }
                ?: throw FileUploadException(
                    "Unsupported file type: ${ALLOWED_EXTENSIONS.joinToString(", ")}"
                )

        fun sniffExtension(bytes: ByteArray): String? = when {
            bytes.startsWith(0xFF, 0xD8, 0xFF) -> "jpg"
            bytes.startsWith(0x89) && bytes.matchesAt(1, "PNG") -> "png"
            bytes.matchesAt(0, "GIF8") -> "gif"
            bytes.matchesAt(0, "RIFF") && bytes.matchesAt(8, "WEBP") -> "webp"
            bytes.matchesAt(0, "%PDF") -> "pdf"
            else -> null
        }

        fun ByteArray.startsWith(vararg prefix: Int): Boolean =
            size >= prefix.size && prefix.indices.all { this[it] == prefix[it].toByte() }

        fun ByteArray.matchesAt(offset: Int, ascii: String): Boolean =
            size >= offset + ascii.length &&
                ascii.indices.all { this[offset + it] == ascii[it].code.toByte() }

        /**
         * Goes into a `Content-Disposition` header, so anything a gallery name might
         * carry — quotes, line breaks, a path, non-latin letters — is dropped rather
         * than left to break the multipart body.
         */
        fun safeFileName(fileName: String?, extension: String): String {
            val base = fileName
                ?.substringAfterLast('/')
                ?.substringAfterLast('\\')
                ?.substringBeforeLast('.')
                ?.filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' || it == '-' }
                ?.take(60)
                ?.ifBlank { null }
                ?: "file"
            return "$base.$extension"
        }
    }
}
