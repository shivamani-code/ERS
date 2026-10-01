package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("EmergencyAppPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_IS_REGISTERED = "is_registered"
    }

    fun saveUser(id: String, token: String, name: String, phone: String) {
        prefs.edit().apply {
            putString(KEY_USER_ID, id)
            putString(KEY_DEVICE_TOKEN, token)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_PHONE, phone)
            putBoolean(KEY_IS_REGISTERED, true)
            apply()
        }
    }

    fun isRegistered(): Boolean = prefs.getBoolean(KEY_IS_REGISTERED, false)
    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""
    fun getDeviceToken(): String = prefs.getString(KEY_DEVICE_TOKEN, "") ?: ""
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun getUserPhone(): String = prefs.getString(KEY_USER_PHONE, "") ?: ""

    fun generateUniqueToken(): String {
        val existing = getDeviceToken()
        if (existing.isNotEmpty()) return existing
        return UUID.randomUUID().toString()
    }
}
