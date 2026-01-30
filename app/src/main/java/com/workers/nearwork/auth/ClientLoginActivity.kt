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
import com.google.firebase.firestore.FirebaseFirestore
import com.workers.nearwork.R
import com.workers.nearwork.client.ClientDashboardActivity

class ClientLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 1. Login Button Click
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            login()
        }

        // 2. Navigation to Sign Up
        findViewById<TextView>(R.id.tvSignUp).setOnClickListener {
            startActivity(Intent(this, ClientRegisterActivity::class.java))
        }

        // 3. Forgot Password
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

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        btnLogin.isEnabled = false
        btnLogin.text = "Verifying..."

        // 1. Auth Check
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid != null) {
                    // 2. Role Check (Client)
                    checkIfClient(uid, btnLogin)
                }
            }
            .addOnFailureListener { e ->
                btnLogin.isEnabled = true
                btnLogin.text = "Login"

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

    private fun checkIfClient(uid: String, btnLogin: Button) {
        // We strictly check the "users" collection (where Clients are stored)
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // SUCCESS: It is a Client account
                    Toast.makeText(this, "Login successful 🎉", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, ClientDashboardActivity::class.java)
                    // Clear back stack
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // FAIL: Account exists, but not in 'users' (so it's probably a Worker)
                    auth.signOut()
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"
                    Toast.makeText(this, "Access Denied: This account is for Workers.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                auth.signOut()
                btnLogin.isEnabled = true
                btnLogin.text = "Login"
                Toast.makeText(this, "Verification failed. Check internet.", Toast.LENGTH_SHORT).show()
            }
    }
}