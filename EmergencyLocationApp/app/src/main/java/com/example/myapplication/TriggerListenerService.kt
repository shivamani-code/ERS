package com.example.myapplication

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentChange

class TriggerListenerService : Service() {

    private val TAG = "TriggerListenerService"
    private val CHANNEL_ID = "TriggerListenerChannel"
    private lateinit var prefs: PrefsHelper
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsHelper(this)
        createNotificationChannel()
        startForeground(2, createNotification())
        startListening()
    }

    private fun startListening() {
        val deviceToken = prefs.getDeviceToken()
        if (deviceToken.isEmpty()) {
            Log.e(TAG, "No device token found. Stopping.")
            stopSelf()
            return
        }

        Log.i(TAG, "Listening for triggers for device: $deviceToken")

        db.collection("app_triggers")
            .whereEqualTo("device_token", deviceToken)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed", e)
                    return@addSnapshotListener
                }

                if (snapshots == null) return@addSnapshotListener

                for (dc in snapshots.documentChanges) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val triggerId = dc.document.id
                        val action = dc.document.getString("action") ?: ""
                        
                        Log.i(TAG, "New Trigger Detected: $triggerId, Action: $action")

                        if (action == "SEND_LOCATION") {
                            // Mark as active
                            db.collection("app_triggers").document(triggerId).update("status", "active")
                            
                            // Launch Location Service
                            val intent = Intent(this, LocationService::class.java).apply {
                                putExtra("trigger_id", triggerId)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent)
                            } else {
                                startService(intent)
                            }
                        }
                    }
                }
            }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Emergency System Monitor")
            .setContentText("Connected and waiting for triggers...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Trigger Listener",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
