package com.dphascow.messenger.repositories

import com.apollographql.apollo.ApolloClient
import com.dphascow.BuildKonfig
import com.dphascow.graphql.RefreshTokenMutation
import com.dphascow.messenger.expects.PlatformLogger
import com.dphascow.messenger.utils.currentTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import settings.AuthPref

class TokenProvider(
    private val authPref: AuthPref
) {
    private val refreshMutex = Mutex()
    private val logger = PlatformLogger("TokenProvider")

    suspend fun accessToken(forceRefresh: Boolean = false): String? {
        val currentToken = authPref.accessToken
        val refreshToken = authPref.refreshToken
        if (currentToken == null) {
            return if (refreshToken == null) null else refreshMutex.withLock { refresh(refreshToken) }
        }
        if (!forceRefresh && isAccessTokenFresh()) return currentToken

        if (refreshToken == null) return currentToken
        return refreshMutex.withLock {
            val tokenAfterWait = authPref.accessToken
            if (!forceRefresh && tokenAfterWait != null && isAccessTokenFresh()) return@withLock tokenAfterWait
            refresh(refreshToken) ?: authPref.accessToken
        }
    }

    suspend fun requireAccessToken(forceRefresh: Boolean = false): String {
        return accessToken(forceRefresh) ?: error("Not authenticated")
    }

    fun saveTokens(accessToken: String, refreshToken: String?) {
        authPref.accessToken = accessToken
        if (refreshToken != null) {
            authPref.refreshToken = refreshToken
        }
    }

    private fun isAccessTokenFresh(): Boolean {
        val updatedAt = authPref.accessTokenUpdatedAt ?: return false
        val refreshBeforeExpiresMs = 60_000L
        val accessTokenTtlMs = 5 * 60_000L
        return currentTimeMillis() - updatedAt < accessTokenTtlMs - refreshBeforeExpiresMs
    }

    private suspend fun refresh(refreshToken: String): String? {
        return try {
            val response = ApolloClient.Builder()
                .serverUrl(BuildKonfig.API_URL)
                .build()
                .mutation(RefreshTokenMutation(refreshToken = refreshToken))
                .execute()

            val payload = response.data?.refreshToken
            val accessToken = payload?.accessToken
            if (accessToken != null) {
                saveTokens(accessToken, payload.refreshToken)
                logger.log("Refresh succeeded")
            } else {
                logger.log("Refresh returned no accessToken: errors=${response.errors} exception=${response.exception}")
            }
            accessToken
        } catch (e: Exception) {
            logger.log("Refresh failed: ${e.message}")
            null
        }
    }
}
