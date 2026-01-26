package com.workers.nearwork.worker

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.workers.nearwork.databinding.ActivityRequestedWorkDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RequestedWorkDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRequestedWorkDetailsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // The ID passed from the previous list screen
    private var workPostId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestedWorkDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 1. Get the Work ID passed from the RecyclerView List
        workPostId = intent.getStringExtra("WORK_POST_ID")

        if (workPostId != null) {
            loadWorkDetails(workPostId!!)
        } else {
            Toast.makeText(this, "Error: Work ID missing", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupListeners()
    }

    private fun loadWorkDetails(postId: String) {
        // Fetch from 'work_posts' collection
        db.collection("work_posts").document(postId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // --- 1. Get Data using EXACT Field Names ---
                    val category = document.getString("category") ?: "General"
                    val description = document.getString("description") ?: "No description"
                    val timestamp = document.getLong("timestamp") ?: System.currentTimeMillis()
                    val clientId = document.getString("clientId")
                    val base64Image = document.getString("imageData")

                    // --- 2. Update UI ---
                    binding.tvCategory.text = category.replaceFirstChar { it.uppercase() }
                    binding.tvDescription.text = description

                    // Format Time (e.g., "2 hours ago")
                    val timeAgo = DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                    binding.tvPostedTime.text = "Posted $timeAgo"

                    // --- 3. Decode and Show Image ---
                    if (!base64Image.isNullOrEmpty()) {
                        decodeBase64Image(base64Image)
                    } else {
                        binding.ivWorkImage.visibility = View.GONE
                    }

                    // --- 4. Fetch Client Name (Secondary Query) ---
                    if (clientId != null) {
                        fetchClientProfile(clientId)
                    }

                } else {
                    Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchClientProfile(clientId: String) {
        // Assuming user details are stored in a 'users' collection
        db.collection("users").document(clientId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("fullName") ?: "Unknown Client"
                    val address = document.getString("address") ?: "Location hidden"

                    binding.tvClientName.text = name
                    binding.tvAddress.text = address

                    // Set Initials (e.g., "John Doe" -> "JD")
                    val initials = name.split(" ")
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .joinToString("")
                        .uppercase()
                    binding.tvUserInitials.text = initials
                }
            }
    }

    private fun decodeBase64Image(base64String: String) {
        try {
            // Convert Base64 String back to Bitmap
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            binding.ivWorkImage.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            binding.ivWorkImage.visibility = View.GONE // Hide if error
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnAccept.setOnClickListener {
            // Logic to Accept Bid
            val bid = binding.etBidAmount.text.toString()
            if(bid.isNotEmpty()) {
                submitBid(bid)
            } else {
                Toast.makeText(this, "Enter a bid amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun submitBid(amount: String) {
        // Update Firestore status or add to a subcollection
        Toast.makeText(this, "Bid Placed: $$amount", Toast.LENGTH_SHORT).show()
    }
}