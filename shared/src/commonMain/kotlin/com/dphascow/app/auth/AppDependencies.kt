package com.dphascow.app.auth

import com.apollographql.apollo.ApolloClient
import com.dphascow.app.business.ApolloBusinessWorkspaceRepository
import com.dphascow.app.business.BusinessWorkspaceRepository
import com.dphascow.app.config.GraphQlLocalConfig
import settings.Prefs

object AppGraphQlConfig {
    var serverUrl: String? = GraphQlLocalConfig.SERVER_URL
}

object AuthFeatureFlags {
    /**
     * Временный макетный режим: не ходим в API даже если serverUrl задан.
     * Отключается одной настройкой, когда появится реальная интеграция.
     */
    var forceMockAuth: Boolean = false

    /**
     * Временный режим для прототипирования экранов: не блокируем flow строгими проверками.
     */
    var disableStrictValidation: Boolean = true
}

object AppDependencies {
    fun createAuthRepository(prefs: Prefs? = null): AuthRepository {
        if (AuthFeatureFlags.forceMockAuth) {
            return MockAuthRepository()
        }

        val serverUrl = AppGraphQlConfig.serverUrl?.takeIf { it.isNotBlank() }
            ?: return MockAuthRepository()

        return ApolloAuthRepository(
            apolloClient = ApolloClient.Builder()
                .serverUrl(serverUrl)
                .build(),
            initialAccessToken = prefs?.accessToken,
        )
    }

    fun createBusinessWorkspaceRepository(prefs: Prefs): BusinessWorkspaceRepository? {
        val serverUrl = AppGraphQlConfig.serverUrl?.takeIf { it.isNotBlank() }
            ?: return null

        return ApolloBusinessWorkspaceRepository(
            apolloClient = ApolloClient.Builder()
                .serverUrl(serverUrl)
                .build(),
            accessTokenProvider = { prefs.accessToken },
        )
    }
}


