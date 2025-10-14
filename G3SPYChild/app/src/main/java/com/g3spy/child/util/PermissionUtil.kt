package com.g3spy.child.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtil {
     
    fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
    
    fun getMissingPermissions(context: Context, permissions: Array<String>): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }
        
        return missingPermissions
    }
    
    fun getReadablePermissionName(permission: String): String {
        return when (permission) {
            android.Manifest.permission.ACCESS_FINE_LOCATION -> "Location"
            android.Manifest.permission.ACCESS_COARSE_LOCATION -> "Approximate Location"
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "Background Location"
            android.Manifest.permission.RECORD_AUDIO -> "Microphone"
            android.Manifest.permission.CAMERA -> "Camera"
            android.Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage"
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage (Write)"
            android.Manifest.permission.READ_PHONE_STATE -> "Phone State"
            android.Manifest.permission.READ_CALL_LOG -> "Call Logs"
            android.Manifest.permission.READ_SMS -> "SMS Messages"
            android.Manifest.permission.RECEIVE_SMS -> "SMS Receiver"
            android.Manifest.permission.ACTIVITY_RECOGNITION -> "Activity Recognition"
            else -> permission.split(".").last()
        }
    }
}