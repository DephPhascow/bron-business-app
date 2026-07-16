package com.dphascow.app.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dphascow.app.push.PushTokenBridge
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import settings.appContext

private const val CHANNEL_ID = "messages"

/**
 * Receives FCM tokens and push messages. Requires Firebase to be configured
 * (google-services.json + the google-services plugin) to actually run.
 */
class AppFirebaseService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        ensureNotificationChannel(applicationContext)
    }

    override fun onNewToken(token: String) {
        PushTokenBridge.submit(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: message.notification?.title ?: "Bron"
        val body = message.data["body"] ?: message.notification?.body.orEmpty()
        showNotification(applicationContext, title, body)
    }
}

fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Incoming messages"
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }
}

private fun showNotification(context: Context, title: String, body: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    ensureNotificationChannel(context)

    val launch = (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pending = PendingIntent.getActivity(
        context,
        0,
        launch,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(context.applicationInfo.icon)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pending)
        .build()

    NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
}
