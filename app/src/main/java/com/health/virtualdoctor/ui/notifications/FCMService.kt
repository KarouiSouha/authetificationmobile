package com.health.virtualdoctor.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.user.UserMetricsActivity

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "🔑 New FCM Token: $token")

        // ✅ Send token to backend
        sendTokenToBackend(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("FCM", "📩 Message received from: ${message.from}")

        // Check if message contains notification payload
        message.notification?.let {
            Log.d("FCM", "📬 Notification Title: ${it.title}")
            Log.d("FCM", "📬 Notification Body: ${it.body}")

            showNotification(it.title ?: "", it.body ?: "")
        }

        // Check if message contains data payload
        message.data.isNotEmpty().let {
            Log.d("FCM", "📦 Data: ${message.data}")

            val type = message.data["type"]
            val action = message.data["action"]

            handleDataPayload(type, action, message.data)
        }
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, UserMetricsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "health_notifications"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications) // ✅ Create this icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Health Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun handleDataPayload(type: String?, action: String?, data: Map<String, String>) {
        when (type) {
            "DOCTOR_REGISTRATION" -> {
                Log.d("FCM", "🩺 New doctor registration notification")
            }
            "DOCTOR_APPROVED" -> {
                Log.d("FCM", "✅ Doctor approved notification")
            }
            else -> {
                Log.d("FCM", "📦 Unknown notification type: $type")
            }
        }
    }

    private fun sendTokenToBackend(token: String) {
        // ✅ TODO: Send FCM token to your backend
        Log.d("FCM", "📤 TODO: Send token to backend: $token")

        // Example:
        // lifecycleScope.launch {
        //     val request = FCMTokenRequest(token, "ANDROID", Build.MODEL)
        //     RetrofitClient.getNotificationService(this).saveFcmToken(request)
        // }
    }
}