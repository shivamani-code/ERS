package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CallReceiver : BroadcastReceiver() {

    private val db = FirebaseFirestore.getInstance()

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive triggered with action: ${intent.action}")
        
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            var incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            Log.i(TAG, "Incoming Call Detected - State: $state, Intent Number: $incomingNumber")

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                if (incomingNumber != null) {
                    Log.i(TAG, "Caller Number Detected: $incomingNumber")
                    uploadToFirestore(incomingNumber)
                } else {
                    Log.w(TAG, "Incoming number null in intent. Attempting CallLog fallback...")
                    fetchNumberFromCallLog(context)
                }
            }
        }
    }

    private fun uploadToFirestore(number: String) {
        Log.d(TAG, "Starting Firestore write for number: $number")
        val callData = hashMapOf(
            "caller_number" to number,
            "timestamp" to Calendar.getInstance().time
        )

        db.collection("incoming_calls")
            .add(callData)
            .addOnSuccessListener { documentReference ->
                Log.i(TAG, "Firestore Write Success! Document ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore Write FAILED for number: $number", e)
            }
    }

    private fun fetchNumberFromCallLog(context: Context) {
        Log.d(TAG, "Querying CallLog...")
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 1"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                    Log.i(TAG, "Successfully fetched number from CallLog: $number")
                    uploadToFirestore(number)
                } else {
                    Log.e(TAG, "CallLog is empty or inaccessible.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error reading CallLog", e)
        }
    }

    companion object {
        private const val TAG = "CallReceiver"
    }
}
