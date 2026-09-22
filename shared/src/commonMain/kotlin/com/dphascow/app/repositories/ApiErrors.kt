package com.dphascow.app.repositories

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.exception.ApolloHttpException
import com.dphascow.BuildKonfig
import com.dphascow.app.resources.*
import com.dphascow.app.resources.Res
import org.jetbrains.compose.resources.getString

/** Header the backend uses to bind a token to one installation. */
const val DEVICE_ID_HEADER = "X-Device-Id"

/**
 * The server binds every access token to `sha256("<device-id>:<user-agent>:<user-id>")`,
 * so a request whose User-Agent differs from the one the token was issued under is
 * rejected as unauthenticated. We talk to the API over two stacks — Apollo for
 * GraphQL and Ktor for file uploads — which default to different agent strings, so
 * both must send this fixed value instead.
 */
val USER_AGENT = "BronBusinessApp/${BuildKonfig.APP_VERSION}"

/**
 * The server rate-limits `requireCode`, `verifyCode`, `tokenAuth` and `refreshToken`;
 * over the limit it answers HTTP 429 instead of a GraphQL error, so callers get this
 * instead of a generic failure and can show a "try again later" message.
 */
class RateLimitException(message: String) : Exception(message)

suspend fun tooManyRequests(): RateLimitException =
    RateLimitException(getString(Res.string.auth_too_many_requests_error))

/** HTTP 429 arrives as a transport exception, not as a GraphQL error. */
fun ApolloResponse<*>.isRateLimited(): Boolean =
    (exception as? ApolloHttpException)?.statusCode == 429

/** Throws [RateLimitException] when the backend refused the call for exceeding a rate limit. */
suspend fun ApolloResponse<*>.failIfRateLimited() {
    if (isRateLimited()) throw tooManyRequests()
}
