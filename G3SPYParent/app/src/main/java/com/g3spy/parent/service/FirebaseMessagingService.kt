package com.g3spy.parent.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.g3spy.parent.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.atomic.AtomicInteger

class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val ALERT_CHANNEL_ID = "g3spy_alerts"
        private val notificationIdCounter = AtomicInteger(1000)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Title: ${notification.title}")
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            
            sendNotification(notification.title, notification.body)
        }
        
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            when (remoteMessage.data["alert_type"]) {
                "geofence_violated" -> {
                    val title = "Geofence Alert"
                    val message = "Child has left the designated safe area"
                    sendNotification(title, message)
                }
                "battery_low" -> {
                    val title = "Battery Alert"
                    val message = "Child device battery is low (${remoteMessage.data["battery_level"]}%)"
                    sendNotification(title, message)
                }
                "suspicious_activity" -> {
                    val title = "Suspicious Activity"
                    val message = remoteMessage.data["message"] ?: "Suspicious activity detected"
                    sendNotification(title, message)
                }
                else -> {
                    
                    val title = remoteMessage.data["title"]
                    val message = remoteMessage.data["message"]
                    if (!title.isNullOrEmpty() && !message.isNullOrEmpty()) {
                        sendNotification(title, message)
                    }
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        if (title.isNullOrEmpty() || messageBody.isNullOrEmpty()) {
            return
        }
        
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "G3SPY Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important alerts from G3SPY monitoring"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = notificationIdCounter.incrementAndGet()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}