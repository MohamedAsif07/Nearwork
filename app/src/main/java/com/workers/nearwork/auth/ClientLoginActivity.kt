package com.workers.nearwork.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.workers.nearwork.MainActivity
import com.workers.nearwork.R
import com.workers.nearwork.client.ClientDashboardActivity // Import your dashboard
import com.workers.nearwork.client.DashboardActivity

class ClientLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_login)

        auth = FirebaseAuth.getInstance()

        // 1. Login Button Click
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            login()
        }

        // 2. Navigation to Sign Up (The link we just added to XML)
        findViewById<TextView>(R.id.tvSignUp).setOnClickListener {
            startActivity(Intent(this, ClientRegisterActivity::class.java))
        }

        // 3. Forgot Password (Optional logic)
        findViewById<TextView>(R.id.tvForgot).setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email).addOnSuccessListener {
                    Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Enter email first to reset password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun login() {
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        val email = etEmail.text.toString().trim()
        val pass = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return
        }
        if (pass.isEmpty()) {
            etPassword.error = "Password is required"
            return
        }

        // Show a loading state on the button
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."

        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                Toast.makeText(this, "Login successful 🎉", Toast.LENGTH_SHORT).show()

                // Redirect to the Client Dashboard instead of MainActivity
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                btnLogin.isEnabled = true
                btnLogin.text = "Login"

                // Specific Error Handling
                when (e) {
                    is FirebaseAuthInvalidUserException -> {
                        etEmail.error = "No account found with this email"
                        etEmail.requestFocus()
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        etPassword.error = "Incorrect password"
                        etPassword.requestFocus()
                    }
                    else -> {
                        Toast.makeText(this, e.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }
}