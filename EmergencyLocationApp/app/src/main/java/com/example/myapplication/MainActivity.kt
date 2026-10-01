package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = PrefsHelper(this)
        
        // Skip registration for testing if user_id exists
        if (prefs.getUserId().isNotEmpty()) {
            startActivity(Intent(this, DashboardActivity::class.java))
        } else {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }
        finish()
    }
}
