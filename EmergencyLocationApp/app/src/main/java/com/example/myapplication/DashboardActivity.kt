package com.example.myapplication

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsHelper
    private val db = FirebaseFirestore.getInstance()

    private lateinit var tvName: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvDeviceId: TextView
    private lateinit var tvGpsStatus: TextView
    private lateinit var tvFirebaseStatus: TextView
    private lateinit var tvSharingStatus: TextView
    private lateinit var tvLastLoc: TextView
    private lateinit var tvLastTime: TextView
    private lateinit var tvDebugLog: TextView

    private val debugEvents = LinkedList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        prefs = PrefsHelper(this)

        tvName = findViewById(R.id.tvProfileName)
        tvPhone = findViewById(R.id.tvProfilePhone)
        tvDeviceId = findViewById(R.id.tvDeviceId)
        tvGpsStatus = findViewById(R.id.tvGpsStatus)
        tvFirebaseStatus = findViewById(R.id.tvFirebaseStatus)
        tvSharingStatus = findViewById(R.id.tvSharingStatus)
        tvLastLoc = findViewById(R.id.tvLastLoc)
        tvLastTime = findViewById(R.id.tvLastTime)
        tvDebugLog = findViewById(R.id.tvDebugLog)

        setupStaticData()
        requestPermissions()
        startBackgroundServices()

        findViewById<MaterialButton>(R.id.btnTestGpsOnly).setOnClickListener {
            runGpsOnlyTest()
        }

        findViewById<MaterialButton>(R.id.btnTestFirestore).setOnClickListener {
            runFirestoreOnlyTest()
        }

        findViewById<MaterialButton>(R.id.btnTestCombined).setOnClickListener {
            runManualCombinedTest()
        }

        findViewById<MaterialButton>(R.id.btnRefresh).setOnClickListener {
            refreshStatus()
        }

        findViewById<MaterialButton>(R.id.btnCopyToken).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Device Token", prefs.getDeviceToken())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Token Copied", Toast.LENGTH_SHORT).show()
        }

        listenToUserUpdates()
        addDebugLog("System Ready")
    }

    private fun addDebugLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$time] $message"
        debugEvents.addFirst(entry)
        if (debugEvents.size > 20) debugEvents.removeLast()
        
        val logText = debugEvents.joinToString("\n")
        runOnUiThread {
            tvDebugLog.text = logText
        }
    }

    private fun runGpsOnlyTest() {
        addDebugLog("TEST 2: GET GPS TEST")
        val gpsHelper = GPSHelper(this)
        
        gpsHelper.requestSingleAccurateLocation(
            onLocationReceived = { location ->
                addDebugLog("GPS SUCCESS: Lat=%.4f, Acc=%.1fm".format(location.latitude, location.accuracy))
                gpsHelper.stopUpdates()
            },
            onError = { error ->
                addDebugLog("GPS FAILED: $error")
            }
        )
    }

    private fun runFirestoreOnlyTest() {
        addLog("TEST 1: SEND FIRESTORE TEST")
        val testData = hashMapOf(
            "test" to true,
            "latitude" to 17.5145286,
            "longitude" to 78.4307839,
            "timestamp" to Date(),
            "user_id" to prefs.getUserId()
        )
        db.collection("locations").add(testData)
            .addOnSuccessListener { addLog("UPLOAD SUCCESS! ID: ${it.id}") }
            .addOnFailureListener { addLog("UPLOAD FAILED: ${it.message}") }
    }

    private fun runManualCombinedTest() {
        addDebugLog("TEST 3: SEND LIVE LOCATION")
        // We use the production service for the combined test to verify it works
        val intent = Intent(this, LocationService::class.java).apply {
            putExtra("is_manual", true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun addLog(msg: String) = addDebugLog(msg)

    private fun setupStaticData() {
        tvName.text = "Name: ${prefs.getUserName()}"
        tvPhone.text = "Phone: ${prefs.getUserPhone()}"
        tvDeviceId.text = "Token: ${prefs.getDeviceToken()}"
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.all { it.value }) addDebugLog("GPS Permissions: GRANTED")
            else addDebugLog("GPS Permissions: DENIED")
        }.launch(permissions.toTypedArray())
    }

    private fun startBackgroundServices() {
        val intent = Intent(this, TriggerListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        addDebugLog("Trigger Listener Service Active")
    }

    private fun listenToUserUpdates() {
        val userId = prefs.getUserId()
        if (userId.isEmpty()) return

        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    tvFirebaseStatus.text = "Firebase: Error"
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    tvFirebaseStatus.text = "Firebase: Connected"
                    
                    val isSharing = snapshot.getBoolean("location_sharing") ?: false
                    tvSharingStatus.text = "Sharing: ${if (isSharing) "ON" else "OFF"}"

                    val lastLoc = snapshot.get("last_location") as? Map<*, *>
                    if (lastLoc != null) {
                        val lat = (lastLoc["lat"] as? Number)?.toDouble() ?: 0.0
                        val lng = (lastLoc["lng"] as? Number)?.toDouble() ?: 0.0
                        val acc = (lastLoc["accuracy"] as? Number)?.toFloat() ?: 0.0f
                        val time = lastLoc["time"] as? com.google.firebase.Timestamp

                        tvLastLoc.text = "Lat: %.6f\nLng: %.6f\nAcc: %.1f m".format(lat, lng, acc)
                        
                        time?.let {
                            val sdf = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
                            tvLastTime.text = "Time: ${sdf.format(it.toDate())}"
                        }
                    }
                }
            }
    }

    private fun refreshStatus() {
        addDebugLog("Refreshing status...")
        listenToUserUpdates()
    }
}
