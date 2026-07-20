import com.apollographql.apollo.ApolloClient
import com.dphascow.app.graphql.RequireCodeMutation
import com.dphascow.app.repositories.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The server hashes the User-Agent into the access token's fingerprint, so every
 * stack we talk to the API with has to send the same one. Left to their defaults
 * Apollo says `okhttp/x.y.z` and Ktor says `ktor-client`, which made uploads fail
 * with 401 while GraphQL worked. These assert the explicit override still wins —
 * an engine upgrade that ignored it would silently break file uploads again.
 */
class UserAgentTest {

    @Test
    fun apolloSendsTheAppUserAgent() {
        val sent = captureUserAgent { url ->
            runBlocking {
                ApolloClient.Builder()
                    .serverUrl(url)
                    .addHttpHeader(HttpHeaders.UserAgent, USER_AGENT)
                    .build()
                    .mutation(RequireCodeMutation(phoneOrEmail = "probe"))
                    .execute()
            }
        }
        assertEquals(USER_AGENT, sent)
    }

    @Test
    fun uploaderSendsTheAppUserAgent() {
        val sent = captureUserAgent { url ->
            runBlocking {
                HttpClient().use { client ->
                    client.submitFormWithBinaryData(
                        url = url,
                        formData = formData { append("file", ByteArray(3)) },
                    ) {
                        header(HttpHeaders.UserAgent, USER_AGENT)
                    }
                }
            }
        }
        assertEquals(USER_AGENT, sent)
    }

    /** Runs [send] against a throwaway local server and returns the User-Agent it saw. */
    private fun captureUserAgent(send: (url: String) -> Unit): String? {
        val server = ServerSocket(0)
        val url = "http://127.0.0.1:${server.localPort}/graphql"
        var userAgent: String? = null
        runBlocking {
            launch(Dispatchers.IO) {
                server.use {
                    it.accept().use { socket ->
                        val reader = socket.getInputStream().bufferedReader()
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            if (line.startsWith("${HttpHeaders.UserAgent}:", ignoreCase = true)) {
                                userAgent = line.substringAfter(':').trim()
                            }
                        }
                        socket.getOutputStream().apply {
                            write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\n{}".toByteArray())
                            flush()
                        }
                    }
                }
            }
            launch(Dispatchers.IO) { runCatching { send(url) } }
        }
        return userAgent
    }
}
