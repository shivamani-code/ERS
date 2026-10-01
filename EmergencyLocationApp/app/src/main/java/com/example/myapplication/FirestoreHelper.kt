package com.example.myapplication

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

object FirestoreHelper {
    private const val TAG = "FirestoreHelper"
    private val db = FirebaseFirestore.getInstance()

    fun registerUser(
        userId: String,
        token: String,
        name: String,
        phone: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val user = hashMapOf(
            "user_id" to userId,
            "name" to name,
            "phone" to phone,
            "device_token" to token,
            "created_at" to Date(),
            "last_location" to null,
            "location_sharing" to false
        )

        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "User registration success in Firestore")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e(TAG, "User registration failed", it)
                onFailure(it)
            }
    }

    fun uploadLocation(
        userId: String,
        phone: String,
        lat: Double,
        lng: Double,
        accuracy: Float,
        triggerId: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val locationData = hashMapOf(
            "user_id" to userId,
            "phone" to phone,
            "latitude" to lat,
            "longitude" to lng,
            "accuracy" to accuracy,
            "timestamp" to Date(),
            "trigger_id" to triggerId
        )

        db.collection("locations").add(locationData)
            .addOnSuccessListener {
                Log.d(TAG, "Location upload success: ${it.id}")
                updateUserLastLocation(userId, lat, lng, accuracy, onSuccess, onFailure)
            }
            .addOnFailureListener {
                Log.e(TAG, "Location upload failed", it)
                onFailure(it)
            }
    }

    private fun updateUserLastLocation(
        userId: String,
        lat: Double,
        lng: Double,
        accuracy: Float,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val update = hashMapOf(
            "last_location" to hashMapOf(
                "lat" to lat,
                "lng" to lng,
                "accuracy" to accuracy,
                "time" to Date()
            ),
            "location_sharing" to false
        )

        db.collection("users").document(userId)
            .update(update as Map<String, Any>)
            .addOnSuccessListener {
                Log.d(TAG, "User last_location updated")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e(TAG, "User update failed", it)
                onFailure(it)
            }
    }
}
