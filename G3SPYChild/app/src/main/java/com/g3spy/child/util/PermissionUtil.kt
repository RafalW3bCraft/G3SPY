package com.g3spy.child.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Utility class to handle permission checks
 */
object PermissionUtil {
    /**
     * Checks if all specified permissions are granted
     * @return true if all permissions are granted, false otherwise
     */
    fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
    
    /**
     * Gets a list of permissions that haven't been granted yet
     * @return List of missing permissions
     */
    fun getMissingPermissions(context: Context, permissions: Array<String>): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }
        
        return missingPermissions
    }
    
    /**
     * Returns a human-readable permission name
     * Useful for displaying permission names to users
     */
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