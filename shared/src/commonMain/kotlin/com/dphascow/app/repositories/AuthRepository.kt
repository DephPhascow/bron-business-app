package com.dphascow.app.repositories

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.dphascow.BuildKonfig
import com.dphascow.app.graphql.AddBusinessMutation
import com.dphascow.app.graphql.MeForAuthQuery
import com.dphascow.app.graphql.TokenAuthMutation

/**
 * Low-level GraphQL client for authentication operations.
 *
 * - Public operations (login, meForAuth with explicit token) use a raw ApolloClient.
 * - Authenticated mutations (addBusiness) go through [Requester] for auto token refresh.
 */
class ApiAuthClient(
    private val requester: Requester,
) {
    private val publicClient: ApolloClient = ApolloClient.Builder()
        .serverUrl(BuildKonfig.API_URL)
        .build()

    suspend fun tokenAuth(
        email: String,
        password: String,
    ): ApolloResponse<TokenAuthMutation.Data> =
        publicClient.mutation(TokenAuthMutation(email = email, password = password)).execute()

    suspend fun meForAuth(token: String): ApolloResponse<MeForAuthQuery.Data> =
        publicClient.query(MeForAuthQuery())
            .addHttpHeader("Authorization", "Bearer $token")
            .execute()

    suspend fun addBusiness(name: String): ApolloResponse<AddBusinessMutation.Data> =
        requester.requestMutation(AddBusinessMutation(name = name))
}
