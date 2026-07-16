package com.dphascow.app.push

import com.dphascow.app.graphql.AddFirebaseTokenMutation
import com.dphascow.app.repositories.Requester

/**
 * Registers the device push (FCM) token with the backend so the server can send
 * notifications. The token itself is obtained on the native side (Firebase) and
 * passed in via [registerToken] — see the push setup guide.
 */
interface PushRepository {
    suspend fun registerToken(token: String)
}

class ApolloPushRepository(
    private val requester: Requester,
) : PushRepository {
    override suspend fun registerToken(token: String) {
        requester.requestMutation(AddFirebaseTokenMutation(token = token))
    }
}
