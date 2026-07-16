package com.dphascow.app.auth

import com.dphascow.app.business.ApolloBusinessWorkspaceRepository
import com.dphascow.app.business.BusinessWorkspaceRepository
import com.dphascow.app.chat.ApolloChatRepository
import com.dphascow.app.chat.ChatRepository
import com.dphascow.app.profile.ApolloProfileRepository
import com.dphascow.app.profile.ProfileRepository
import com.dphascow.app.push.ApolloPushRepository
import com.dphascow.app.push.PushRepository
import com.dphascow.app.repositories.ApiAuthClient
import com.dphascow.app.repositories.FileUploader
import com.dphascow.app.repositories.Requester
import settings.Prefs

object AppDependencies {
    fun createRequester(prefs: Prefs, onAuthError: () -> Unit): Requester =
        Requester(authPref = prefs, onAuthenticateError = onAuthError)

    fun createAuthRepository(requester: Requester): AuthRepository =
        ApolloAuthRepository(ApiAuthClient(requester))

    fun createMockAuthRepository(): AuthRepository = MockAuthRepository()

    fun createBusinessWorkspaceRepository(requester: Requester): BusinessWorkspaceRepository =
        ApolloBusinessWorkspaceRepository(requester, ApiAuthClient(requester), FileUploader())

    fun createProfileRepository(requester: Requester): ProfileRepository =
        ApolloProfileRepository(requester, FileUploader())

    fun createChatRepository(requester: Requester, prefs: Prefs): ChatRepository =
        ApolloChatRepository(requester, FileUploader(), prefs)

    fun createPushRepository(requester: Requester): PushRepository =
        ApolloPushRepository(requester)
}


