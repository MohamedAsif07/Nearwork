package com.workers.nearwork.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.workers.nearwork.R
import com.workers.nearwork.databinding.ActivityWorkerLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.workers.nearwork.client.DashboardActivity
import com.workers.nearwork.worker.WorkerHomeActivity

class WorkerLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkerLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

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
            binding.btnLogin.text = "Logging in..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, WorkerHomeActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Login"
                    Toast.makeText(this, "Login Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
}