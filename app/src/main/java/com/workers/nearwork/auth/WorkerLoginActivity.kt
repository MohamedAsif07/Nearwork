package com.workers.nearwork.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.workers.nearwork.R
import com.workers.nearwork.databinding.ActivityWorkerLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.workers.nearwork.worker.WorkerHomeActivity
import com.workers.nearwork.worker.WorkerRegistrationActivity

class WorkerLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkerLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupListeners()
    }

    private fun setupListeners() {
        // Password Visibility Toggle
        binding.btnLoginTogglePass.setOnClickListener {
            val selection = binding.etLoginPassword.selectionEnd
            if (binding.etLoginPassword.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                binding.etLoginPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnLoginTogglePass.setImageResource(R.drawable.ic_visibility)
            } else {
                binding.etLoginPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnLoginTogglePass.setImageResource(R.drawable.ic_visibility)
            }
            binding.etLoginPassword.setSelection(selection)
        }

        // Login Button
        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.toString().trim()
            val password = binding.etLoginPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Verifying..."

            // 1. First, check Authentication (Email/Password matches)
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid
                    if (uid != null) {
                        // 2. Second, check if they are actually a WORKER
                        checkIfWorker(uid)
                    }
                }
                .addOnFailureListener { e ->
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Login"
                    Toast.makeText(this, "Login Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Navigate to Register
        binding.tvContactSupport.setOnClickListener {
            val intent = Intent(this, WorkerRegistrationActivity::class.java)
            startActivity(intent)
        }

        // Biometric Placeholder
        binding.btnBiometric.setOnClickListener {
            Toast.makeText(this, "Biometric feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Forgot Password Placeholder
        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Reset password feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkIfWorker(uid: String) {
        // We strictly check the "workers" collection
        db.collection("workers").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // SUCCESS: It is a worker account
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@WorkerLoginActivity, WorkerHomeActivity::class.java)
                    // Clear back stack so they can't go back to login
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // FAIL: Email exists, but it's likely a Client account
                    auth.signOut() // Log them out
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Login"
                    Toast.makeText(this, "Access Denied: This account is not a Worker.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                auth.signOut()
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Login"
                Toast.makeText(this, "Verification Error. Try again.", Toast.LENGTH_SHORT).show()
            }
    }
}