package com.dphascow.app.repositories

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.dphascow.BuildKonfig
import com.dphascow.app.graphql.AddBusinessMutation
import com.dphascow.app.graphql.LogoutAllDevicesMutation
import com.dphascow.app.graphql.LogoutMutation
import com.dphascow.app.graphql.MeForAuthQuery
import com.dphascow.app.graphql.RequireCodeMutation
import com.dphascow.app.graphql.VerifyCodeMutation

/**
 * Low-level GraphQL client for authentication operations.
 *
 * - Public operations (requireCode, verifyCode, meForAuth with explicit token) use a raw ApolloClient.
 * - Authenticated mutations (addBusiness) go through [Requester] for auto token refresh.
 */
class ApiAuthClient(
    private val requester: Requester,
    /** Sent even on the login calls — the server binds the issued token to it. */
    private val deviceId: String,
) {
    private val publicClient: ApolloClient = ApolloClient.Builder()
        .serverUrl(BuildKonfig.API_URL)
        .addHttpHeader(DEVICE_ID_HEADER, deviceId)
        // The login response's token is fingerprinted with this exact agent string,
        // so every later request has to send the same one.
        .addHttpHeader("User-Agent", USER_AGENT)
        .build()

    /** @throws RateLimitException when the code was requested too often (3 per 5 min). */
    suspend fun requireCode(
        phoneOrEmail: String,
    ): ApolloResponse<RequireCodeMutation.Data> =
        publicClient.mutation(RequireCodeMutation(phoneOrEmail = phoneOrEmail)).execute()
            .also { it.failIfRateLimited() }

    /** @throws RateLimitException when the code was checked too often (10 per 5 min). */
    suspend fun verifyCode(
        phoneOrEmail: String,
        code: String,
    ): ApolloResponse<VerifyCodeMutation.Data> =
        publicClient.mutation(VerifyCodeMutation(phoneOrEmail = phoneOrEmail, code = code)).execute()
            .also { it.failIfRateLimited() }

    suspend fun meForAuth(token: String): ApolloResponse<MeForAuthQuery.Data> =
        publicClient.query(MeForAuthQuery())
            .addHttpHeader("Authorization", "Bearer $token")
            .execute()

    suspend fun addBusiness(name: String): ApolloResponse<AddBusinessMutation.Data> =
        requester.requestMutation(AddBusinessMutation(name = name))

    /** Invalidates the refresh token of this device's session. */
    suspend fun logout(): ApolloResponse<LogoutMutation.Data> =
        requester.requestMutation(LogoutMutation())

    /** Invalidates the refresh tokens of every session this user has. */
    suspend fun logoutAllDevices(): ApolloResponse<LogoutAllDevicesMutation.Data> =
        requester.requestMutation(LogoutAllDevicesMutation())
}
