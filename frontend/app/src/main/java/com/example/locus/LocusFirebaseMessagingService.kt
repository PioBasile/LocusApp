package com.example.locus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.locus.data.remote.FCMTokenRequest
import com.example.locus.data.remote.RetrofitClient
import com.example.locus.utils.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocusFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val jwt = SessionManager(this).token ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try { RetrofitClient.api.updateFCMToken(jwt, FCMTokenRequest(token)) } catch (_: Exception) { }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: return
        val body  = remoteMessage.notification?.body  ?: return

        // Notification format: title = likerUsername, body = "liked your post"
        // Suppress if the liker is the currently logged-in user (same device, different accounts)
        val myUsername = SessionManager(this).username?.trim()
        if (!myUsername.isNullOrBlank() && title.equals(myUsername, ignoreCase = true) && body.contains("liked your post", ignoreCase = true)) return

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "locus_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "Locus", NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
