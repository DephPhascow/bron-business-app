package com.dphascow.app.repositories

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Query
import com.dphascow.BuildKonfig

/**
 * Лёгкий Apollo requester без Authorization.
 * Нужен для публичных запросов, например `version`.
 */
class PublicRequester {
    private val client: ApolloClient = ApolloClient.Builder()
        .serverUrl(BuildKonfig.API_URL)
        .build()

    suspend fun <D : Query.Data, Q : Query<D>> requestQuery(query: Q): ApolloResponse<D> =
        client.query(query).execute()
}
