package com.dphascow.app.repositories

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.network.websocket.GraphQLWsProtocol
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
import com.dphascow.BuildKonfig
import com.dphascow.app.utils.PlatformLogger
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import settings.AuthPref
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class Requester(
    private val authPref: AuthPref,
    private val onAuthenticateError: () -> Unit
) {
    private val logger = PlatformLogger("GRAPHQL")

    /** Shared so file uploads reuse the same token (and the same single-flight refresh). */
    val tokenProvider = TokenProvider(authPref)

    private fun buildApollo(accessToken: String?): ApolloClient {
        logger.log(BuildKonfig.API_URL)

        var builder = ApolloClient.Builder()
            .serverUrl(BuildKonfig.API_URL)
            // Tokens are bound to the device, not the IP — without this header every
            // authenticated request is rejected.
            .addHttpHeader(DEVICE_ID_HEADER, authPref.deviceId)

        if (accessToken != null) {
            builder = builder.addHttpHeader("Authorization", "Bearer $accessToken")
        }

        return builder.build()
    }

    suspend fun <D : Query.Data, Q : Query<D>> requestQuery(query: Q) =
        execute { it.query(query).execute() }

    suspend fun <D : Mutation.Data, Q : Mutation<D>> requestMutation(mutation: Q) =
        execute { it.mutation(mutation).execute() }

    @OptIn(ApolloExperimental::class)
    private fun buildSubscriptionClient(): ApolloClient {
        val transport = WebSocketNetworkTransport.Builder()
            .serverUrl(BuildKonfig.WS_URL)
            .wsProtocol(
                GraphQLWsProtocol(
                    connectionPayload = {
                        val token = tokenProvider.accessToken(forceRefresh = true)
                        logger.log("WS connection_init auth token present=${token != null}")
                        if (token == null) emptyMap<String, String>() else mapOf("Authorization" to "Bearer $token")
                    }
                )
            )
            .build()

        return ApolloClient.Builder()
            .serverUrl(BuildKonfig.API_URL)
            .addHttpHeader(DEVICE_ID_HEADER, authPref.deviceId)
            .subscriptionNetworkTransport(transport)
            .build()
    }

    @OptIn(ApolloExperimental::class)
    fun <D : Subscription.Data, S : Subscription<D>> requestSubscription(subscription: S): Flow<ApolloResponse<D>> {
        return buildSubscriptionClient().subscription(subscription)
            .toFlow()
            .onStart { logger.log("Subscription started ${subscription.name()}") }
            .catch { logger.log("Subscription error $it") }
    }

    // ---------------- EXECUTE ----------------

    private suspend fun <T : Operation.Data> execute(
        call: suspend (ApolloClient) -> ApolloResponse<T>
    ): ApolloResponse<T> {

        var client = buildApollo(tokenProvider.accessToken())
        var response = call(client)

        // 429 is a transport-level refusal, not an expired token — retrying with a
        // fresh token would only burn another slot in the limit window.
        response.failIfRateLimited()

        // Если токен умер → refresh → retry
        if (response.hasAuthError()) {
            val newToken = tokenProvider.accessToken(forceRefresh = true)
            if (newToken == null) {
                triggerAuthError()
                return response
            }

            client = buildApollo(newToken)
            response = call(client)
            response.failIfRateLimited()
        }

        // Если второй раз 100 → logout
        if (response.hasAuthError()) {
            triggerAuthError()
        } else {
            // Arm the latch again: without this, the next expired session after a
            // re-login would never reach the coordinator and the app would sit on a
            // dead token instead of showing the sign-in screen.
            authOnce.store(false)
        }

        return response
    }

    // ---------------- AUTH CALLBACK SAFE ----------------

    private val authOnce = AtomicBoolean(false)

    private fun triggerAuthError() {
        if (authOnce.compareAndSet(expectedValue = false, newValue = true)) {
            MainScope().launch {
                onAuthenticateError()
            }
        }
    }

    // ---------------- UTILS ----------------

    private fun ApolloResponse<*>.hasAuthError(): Boolean {
        return errors?.any { error ->
            error.extensions?.get("code") == 100 ||
                error.message.contains("Неавторизован", ignoreCase = true) ||
                error.message.contains("Unauthorized", ignoreCase = true) ||
                error.message.contains("Unauthenticated", ignoreCase = true)
        } == true
    }
}
