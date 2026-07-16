package com.dphascow.app.repositories

import com.dphascow.BuildKonfig
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

/**
 * Uploads raw file bytes to the REST `/files/upload` endpoint (multipart) and
 * returns the stored public URL, which is then persisted via GraphQL
 * (`business.logoUrl`, gallery image, …).
 */
class FileUploader {
    suspend fun uploadImage(bytes: ByteArray, fileName: String?, mimeType: String?): String {
        val client = HttpClient()
        try {
            val response = client.submitFormWithBinaryData(
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
            )
            val text = response.bodyAsText()
            return URL_REGEX.find(text)?.groupValues?.get(1)
                ?: throw IllegalStateException("Upload failed: $text")
        } finally {
            client.close()
        }
    }

    private companion object {
        val URL_REGEX = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"")
    }
}
