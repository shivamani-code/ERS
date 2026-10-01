package com.example.myapplication

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class LocationService : Service() {

    private val TAG = "LocationService"
    private lateinit var gpsHelper: GPSHelper
    private lateinit var prefs: PrefsHelper
    private val CHANNEL_ID = "LocationServiceChannel"

    override fun onCreate() {
        super.onCreate()
        gpsHelper = GPSHelper(this)
        prefs = PrefsHelper(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val triggerId = intent?.getStringExtra("trigger_id")
        val isManual = intent?.getBooleanExtra("is_manual", false) ?: false

        Log.i(TAG, "Starting Location Acquisition. Manual=$isManual, Trigger=$triggerId")
        
        startForeground(1, createNotification(isManual))

        gpsHelper.requestSingleAccurateLocation(
            onLocationReceived = { location ->
                Log.i(TAG, "Acquired Location: ${location.latitude}, ${location.longitude}")
                
                FirestoreHelper.uploadLocation(
                    userId = prefs.getUserId(),
                    phone = prefs.getUserPhone(),
                    lat = location.latitude,
                    lng = location.longitude,
                    accuracy = location.accuracy,
                    triggerId = triggerId,
                    onSuccess = {
                        Log.i(TAG, "Location successfully dispatched")
                        stopSelf()
                    },
                    onFailure = {
                        Log.e(TAG, "Failed to dispatch location: ${it.message}")
                        stopSelf()
                    }
                )
            },
            onError = { error ->
                Log.e(TAG, "GPS Error: $error")
                stopSelf()
            }
        )

        return START_NOT_STICKY
    }

    private fun createNotification(isManual: Boolean): Notification {
        val title = if (isManual) "Testing GPS Sensor" else "Emergency Location Active"
        val text = "Acquiring precision coordinates..."
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Location Service",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        gpsHelper.stopUpdates()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
