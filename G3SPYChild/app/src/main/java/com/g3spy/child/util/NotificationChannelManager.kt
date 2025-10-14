package com.g3spy.child.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

object NotificationChannelManager {
     
    fun createNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannelInternal(context, channelId, channelName, importance)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannelInternal(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int
    ) {
        val channel = NotificationChannel(
            channelId,
            channelName,
            importance
        ).apply {
            description = "Used for G3SPY service notifications"
            setShowBadge(false)
        }
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}