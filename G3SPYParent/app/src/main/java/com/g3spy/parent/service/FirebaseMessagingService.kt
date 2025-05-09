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

/**
 * Service to handle Firebase Cloud Messaging for notifications
 */
class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val ALERT_CHANNEL_ID = "g3spy_alerts"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Title: ${notification.title}")
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            
            // Display the notification
            sendNotification(notification.title, notification.body)
        }
        
        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            // Handle different alert types
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
                    // Handle generic notifications
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
        
        // If needed, send the token to your server
        // You can use this token to send messages directly to this app instance
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        if (title.isNullOrEmpty() || messageBody.isNullOrEmpty()) {
            return
        }
        
        // Create an intent to open the main activity when the notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Set up the notification appearance
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

        // Create the notification channel for Android O and above
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

        // Show the notification
        val notificationId = (System.currentTimeMillis() / 1000).toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}