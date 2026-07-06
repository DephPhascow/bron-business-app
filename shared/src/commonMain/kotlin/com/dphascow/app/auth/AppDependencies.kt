package com.dphascow.app.auth

import com.dphascow.app.business.ApolloBusinessWorkspaceRepository
import com.dphascow.app.business.BusinessWorkspaceRepository
import com.dphascow.app.repositories.ApiAuthClient
import com.dphascow.app.repositories.Requester
import settings.Prefs

object AppDependencies {
    fun createRequester(prefs: Prefs, onAuthError: () -> Unit): Requester =
        Requester(authPref = prefs, onAuthenticateError = onAuthError)

    fun createAuthRepository(requester: Requester): AuthRepository =
        ApolloAuthRepository(ApiAuthClient(requester))

    fun createMockAuthRepository(): AuthRepository = MockAuthRepository()

    fun createBusinessWorkspaceRepository(requester: Requester): BusinessWorkspaceRepository =
        ApolloBusinessWorkspaceRepository(requester)
}


