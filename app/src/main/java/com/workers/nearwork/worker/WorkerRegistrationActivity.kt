package com.workers.nearwork.worker

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.workers.nearwork.MainActivity
import com.workers.nearwork.auth.WorkerLoginActivity
import com.workers.nearwork.databinding.ActivityWorkerRegistrationBinding
import java.io.ByteArrayOutputStream

// Ensure you import R from your package.
// If R is red, make sure the package in AndroidManifest.xml matches or import com.workers.nearwork.R

class WorkerRegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerRegistrationBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var proofBase64: String? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.ivProofPreview.setImageURI(it)
            binding.tvUploadHint.text = "Proof Selected"
            proofBase64 = encodeImageToBase64(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkerRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupSpinner()
        setupListeners()
    }

    private fun setupSpinner() {
        val domains = arrayOf("Select your domain", "Plumber", "Electrician", "Carpenter", "Painter", "Cleaner", "Mechanic")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, domains)
        binding.spinnerDomain.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Toggle Main Password
        setupPasswordToggle(binding.etPassword, binding.btnTogglePassword)

        // Toggle Confirm Password
        setupPasswordToggle(binding.etConfirmPassword, binding.btnToggleConfirmPassword)

        binding.layoutUploadProof.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.btnRegister.setOnClickListener {
            if (validateInputs()) {
                registerWorker()
            }
        }
    }

    // Helper to toggle password visibility
    private fun setupPasswordToggle(editText: EditText, toggleIcon: ImageView) {
        toggleIcon.setOnClickListener {
            val selection = editText.selectionEnd
            if (editText.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleIcon.setImageResource(com.workers.nearwork.R.drawable.ic_visibility)
            } else {
                editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggleIcon.setImageResource(com.workers.nearwork.R.drawable.ic_visibility)
            }
            editText.setSelection(selection)
        }
    }

    private fun validateInputs(): Boolean {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (fullName.isEmpty()) {
            binding.etFullName.error = "Name required"
            return false
        }
        if (email.isEmpty()) {
            binding.etEmail.error = "Email required"
            return false
        }
        if (password.length < 6) {
            binding.etPassword.error = "Min 6 chars"
            return false
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.spinnerDomain.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select a domain", Toast.LENGTH_SHORT).show()
            return false
        }
        if (proofBase64 == null) {
            Toast.makeText(this, "Please upload ID proof", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun registerWorker() {
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Registering..."

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                saveWorkerData(uid!!)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Auth Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "Register"
            }
    }

    private fun saveWorkerData(uid: String) {
        val workerData = hashMapOf(
            "uid" to uid,
            "fullName" to binding.etFullName.text.toString().trim(),
            "email" to binding.etEmail.text.toString().trim(),
            "address" to binding.etAddress.text.toString().trim(),
            "doorNo" to binding.etDoorNo.text.toString().trim(),
            "pincode" to binding.etPincode.text.toString().trim(),
            "experience" to binding.etExperience.text.toString().trim(),
            "domain" to binding.spinnerDomain.selectedItem.toString(),
            "proofImageBase64" to proofBase64,
            "userType" to "worker",
            "isVerified" to false
        )

        // Storing in 'workers' collection
        db.collection("workers").document(uid)
            .set(workerData)
            .addOnSuccessListener {
                Toast.makeText(this, "Worker Registered Successfully!", Toast.LENGTH_SHORT).show()
                // Ensure WorkerDashboardActivity is also in the correct package
                startActivity(Intent(this, WorkerLoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "DB Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "Register"
            }
    }

    private fun encodeImageToBase64(imageUri: Uri): String {
        val inputStream = contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}