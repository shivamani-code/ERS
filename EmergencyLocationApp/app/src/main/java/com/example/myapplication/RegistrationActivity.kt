package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegistrationActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var pbLoading: ProgressBar
    private lateinit var prefs: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        prefs = PrefsHelper(this)

        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        btnRegister = findViewById(R.id.btnRegister)
        pbLoading = findViewById(R.id.pbLoading)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performRegistration(name, phone)
        }
    }

    private fun performRegistration(name: String, phone: String) {
        setLoading(true)
        
        val userId = prefs.generateUniqueId()
        val deviceToken = prefs.generateUniqueId()

        FirestoreHelper.registerUser(
            userId = userId,
            token = deviceToken,
            name = name,
            phone = phone,
            onSuccess = {
                prefs.saveUser(userId, deviceToken, name, phone)
                setLoading(false)
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            },
            onFailure = {
                setLoading(false)
                Toast.makeText(this, "Registration Failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setLoading(loading: Boolean) {
        pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !loading
        etName.isEnabled = !loading
        etPhone.isEnabled = !loading
    }
}
