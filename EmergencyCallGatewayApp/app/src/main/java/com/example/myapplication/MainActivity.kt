package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.FOREGROUND_SERVICE
        )
    } else {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startMonitoringService()
        } else {
            Toast.makeText(this, "Permissions required for call monitoring", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        // Aggressively check and request permissions on start
        checkAndRequestPermissions()

        btnStart.setOnClickListener {
            if (checkPermissions()) {
                startMonitoringService()
                tvStatus.text = "Status: Running"
                Toast.makeText(this, "Monitoring Started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions NOT granted. Requesting...", Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(requiredPermissions)
            }
        }

        btnStop.setOnClickListener {
            stopMonitoringService()
            tvStatus.text = "Status: Stopped"
        }

        // Test Firebase connection on startup
        testFirebaseConnection()
    }

    private fun testFirebaseConnection() {
        Log.d("GatewayApp", "Testing Firebase Connection...")
        val testData = hashMapOf(
            "test" to "connection_test",
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("connection_tests").add(testData)
            .addOnSuccessListener {
                Log.i("GatewayApp", "Firebase Connection Successful: Document ID ${it.id}")
                Toast.makeText(this, "Firebase Connection OK", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("GatewayApp", "Firebase Connection Failed", e)
                Toast.makeText(this, "Firebase Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, CallMonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopMonitoringService() {
        val intent = Intent(this, CallMonitoringService::class.java)
        stopService(intent)
    }

    private fun checkAndRequestPermissions() {
        if (!checkPermissions()) {
            Log.d("GatewayApp", "Permissions missing. Triggering launcher...")
            requestPermissionLauncher.launch(requiredPermissions)
        } else {
            Log.d("GatewayApp", "All permissions already granted.")
        }
    }
}
