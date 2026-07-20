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

    fun createAuthRepository(requester: Requester, prefs: Prefs): AuthRepository =
        ApolloAuthRepository(ApiAuthClient(requester, prefs.deviceId))

    fun createMockAuthRepository(): AuthRepository = MockAuthRepository()

    fun createBusinessWorkspaceRepository(requester: Requester, prefs: Prefs): BusinessWorkspaceRepository =
        ApolloBusinessWorkspaceRepository(requester, fileUploader(requester, prefs))

    fun createProfileRepository(requester: Requester, prefs: Prefs): ProfileRepository =
        ApolloProfileRepository(requester, fileUploader(requester, prefs))

    fun createChatRepository(requester: Requester, prefs: Prefs): ChatRepository =
        ApolloChatRepository(requester, fileUploader(requester, prefs), prefs)

    private fun fileUploader(requester: Requester, prefs: Prefs): FileUploader =
        FileUploader(requester.tokenProvider, prefs.deviceId)

    fun createPushRepository(requester: Requester): PushRepository =
        ApolloPushRepository(requester)
}


