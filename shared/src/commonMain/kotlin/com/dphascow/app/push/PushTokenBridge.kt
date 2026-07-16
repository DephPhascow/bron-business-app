package com.dphascow.app.push

/**
 * Seam between the native Firebase layer and shared code.
 *
 * Shared code sets [handler] once a session is ready; the platform Firebase
 * integration (FCM service on Android, APNs/Firebase on iOS) calls [submit]
 * whenever a push token becomes available (on launch and on token refresh).
 */
object PushTokenBridge {
    var handler: ((String) -> Unit)? = null

    fun submit(token: String) {
        handler?.invoke(token)
    }
}
