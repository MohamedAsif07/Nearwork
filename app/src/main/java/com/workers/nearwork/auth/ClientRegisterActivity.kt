package com.workers.nearwork.auth

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.workers.nearwork.R

class ClientRegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 1. Initialize Views (including Confirm Password)
        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etAddress = findViewById<EditText>(R.id.etAddress)
        val etDoorNumber = findViewById<EditText>(R.id.etDoorNumber)
        val etPincode = findViewById<EditText>(R.id.etPincode)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)
        val ivPassToggle = findViewById<ImageView>(R.id.ivPasswordToggle)
        val ivConfirmPassToggle = findViewById<ImageView>(R.id.ivConfirmPasswordToggle)

        // Password Visibility Toggles
        setupPasswordToggle(etPassword, ivPassToggle)
        setupPasswordToggle(etConfirmPassword, ivConfirmPassToggle)

        // 2. Register Button Logic
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val name = etFullName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val door = etDoorNumber.text.toString().trim()
            val pincode = etPincode.text.toString().trim()

            // Basic Validation
            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // NEW: Check if passwords match
            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            // Show a simple progress message
            btnRegister.isEnabled = false
            btnRegister.text = "Registering..."

            // 3. Create User in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid
                    if (uid != null) {
                        saveClientData(uid, name, email, phone, address, door, pincode)
                    }
                }
                .addOnFailureListener { e ->
                    btnRegister.isEnabled = true
                    btnRegister.text = "Register"

                    // NEW: Check for existing email error
                    if (e is FirebaseAuthUserCollisionException) {
                        etEmail.error = "This email is already registered"
                        etEmail.requestFocus()
                    } else {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvSignIn.setOnClickListener {
            finish()
        }
    }

    private fun setupPasswordToggle(editText: EditText, imageView: ImageView) {
        var isVisible = false
        imageView.setOnClickListener {
            if (isVisible) {
                editText.transformationMethod = PasswordTransformationMethod.getInstance()
                imageView.setImageResource(R.drawable.ic_visibility) // change to your eye-off icon if you have one
                isVisible = false
            } else {
                editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                imageView.setImageResource(R.drawable.ic_visibility) // change to eye-on icon
                isVisible = true
            }
            editText.setSelection(editText.text.length)
        }
    }

    private fun saveClientData(uid: String, name: String, email: String, phone: String, address: String, door: String, pincode: String) {
        val data = hashMapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "address" to address,
            "doorNo" to door,
            "pincode" to pincode,
            "role" to "client",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(uid)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                findViewById<Button>(R.id.btnRegister).isEnabled = true
                Toast.makeText(this, "Failed to save profile data", Toast.LENGTH_SHORT).show()
            }
    }
}