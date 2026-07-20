package com.dphascow.app.repositories

import com.dphascow.BuildKonfig
import io.ktor.client.HttpClient
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
    /** Convenience for the callers that only need somewhere to point an `imageUrl` at. */
    suspend fun uploadImage(bytes: ByteArray, fileName: String?, mimeType: String?): String =
        upload(bytes, fileName, mimeType).url

    suspend fun upload(bytes: ByteArray, fileName: String?, mimeType: String?): UploadedFile {
        if (bytes.size > MAX_UPLOAD_BYTES) {
            throw FileUploadException("File is larger than ${MAX_UPLOAD_BYTES / 1_000_000} MB")
        }

        val client = HttpClient()
        try {
            var response = client.post(bytes, fileName, mimeType, requireToken(forceRefresh = false))
            // Access tokens live 5 minutes. GraphQL calls refresh and retry on their
            // own, and uploads have to do the same or a stale token surfaces to the
            // user as "not authenticated" mid-session.
            if (response.status.value == 401) {
                response = client.post(bytes, fileName, mimeType, requireToken(forceRefresh = true))
            }

            val text = response.bodyAsText()
            when (response.status.value) {
                401 -> throw IllegalStateException("Not authenticated")
                413 -> throw FileUploadException("File is too large (max ${MAX_UPLOAD_BYTES / 1_000_000} MB)")
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
        fileName: String?,
        mimeType: String?,
        token: String,
    ): HttpResponse = submitFormWithBinaryData(
        url = "${BuildKonfig.API_API}/files/upload",
        formData = formData {
            append(
                key = "file",
                value = bytes,
                headers = Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"${fileName ?: "photo.jpg"}\"")
                    append(HttpHeaders.ContentType, mimeType ?: "application/octet-stream")
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
        const val MAX_UPLOAD_BYTES = 20 * 1_000_000
        val ALLOWED_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp", "gif", "pdf")

        /** The response is a flat JSON object, so a field-level regex is enough. */
        fun jsonField(name: String) = Regex("\"$name\"\\s*:\\s*\"([^\"]+)\"")
    }
}
