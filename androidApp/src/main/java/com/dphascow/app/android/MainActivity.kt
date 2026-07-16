package com.dphascow.app.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.dphascow.app.App
import com.dphascow.app.push.PushTokenBridge
import com.google.firebase.messaging.FirebaseMessaging
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import settings.appContext

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = this.applicationContext
        FileKit.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Safe before google-services.json is added: throws if Firebase isn't configured, which we ignore.
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                PushTokenBridge.submit(token)
            }
        }

        setContent {
            App()
        }
    }
}
