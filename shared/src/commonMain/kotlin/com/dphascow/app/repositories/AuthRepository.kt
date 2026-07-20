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
import com.dphascow.app.graphql.VerifyEmployeeCodeMutation

/**
 * Low-level GraphQL client for authentication operations.
 *
 * - Public operations (requireCode, verifyCode, meForAuth with explicit token) use a raw ApolloClient.
 * - Authenticated mutations (addBusiness) go through [Requester] for auto token refresh.
 */
class ApiAuthClient(
    private val requester: Requester,
) {
    private val publicClient: ApolloClient = ApolloClient.Builder()
        .serverUrl(BuildKonfig.API_URL)
        .build()

    suspend fun requireCode(
        phoneOrEmail: String,
    ): ApolloResponse<RequireCodeMutation.Data> =
        publicClient.mutation(RequireCodeMutation(phoneOrEmail = phoneOrEmail)).execute()

    suspend fun verifyCode(
        phoneOrEmail: String,
        code: String,
    ): ApolloResponse<VerifyCodeMutation.Data> =
        publicClient.mutation(VerifyCodeMutation(phoneOrEmail = phoneOrEmail, code = code)).execute()

    /**
     * Confirms an employee's phone during an invite. Uses the same server-side
     * `verifyCode` but selects only the user id — the employee's tokens are never
     * requested, so the manager's session is untouched.
     */
    suspend fun verifyEmployeeCode(
        phoneOrEmail: String,
        code: String,
    ): ApolloResponse<VerifyEmployeeCodeMutation.Data> =
        publicClient.mutation(VerifyEmployeeCodeMutation(phoneOrEmail = phoneOrEmail, code = code)).execute()

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
