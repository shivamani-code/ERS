package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class GPSHelper(private val context: Context) {
    private val TAG = "GPSHelper"
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val settingsClient = LocationServices.getSettingsClient(context)
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun requestSingleAccurateLocation(
        onLocationReceived: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "Starting GPS Request with Settings Check...")

        // 1. Define the location request we want
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setWaitForAccurateLocation(false)
            .build()

        // 2. Check if current device settings satisfy this request
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // Crucial for showing the dialog even if GPS is off

        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // All location settings are satisfied. The client can initialize location requests here.
            Log.d(TAG, "Location settings are satisfied. Starting updates.")
            startActualUpdates(locationRequest, onLocationReceived, onError)
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                try {
                    Log.w(TAG, "Location settings NOT satisfied. Attempting to resolve...")
                    // We need an activity context to show the dialog
                    if (context is Activity) {
                        exception.startResolutionForResult(context, 1001)
                        onError("Location is OFF. Please accept the system dialog to turn it ON.")
                    } else {
                        onError("Location is OFF. Please enable it in system settings.")
                    }
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, "Error starting resolution", sendEx)
                    onError("Could not prompt for GPS activation.")
                }
            } else {
                Log.e(TAG, "Location settings check failed without resolution", exception)
                onError("GPS hardware unavailable or disabled.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startActualUpdates(
        locationRequest: LocationRequest,
        onLocationReceived: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        var bestLocation: Location? = null

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val accuracy = location.accuracy

                Log.d(TAG, "GPS Update: Lat=${location.latitude}, Acc=${accuracy}m")

                if (bestLocation == null || accuracy < bestLocation!!.accuracy) {
                    bestLocation = location
                }
                
                if (accuracy <= 20.0f) {
                    stopUpdates()
                    onLocationReceived(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )

        // Timeout fallback
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            if (locationCallback != null) {
                stopUpdates()
                if (bestLocation != null) {
                    onLocationReceived(bestLocation!!)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) onLocationReceived(lastLoc)
                        else onError("GPS Timeout: No location acquired.")
                    }
                }
            }
        }, 15000L)
    }

    fun stopUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }
}
