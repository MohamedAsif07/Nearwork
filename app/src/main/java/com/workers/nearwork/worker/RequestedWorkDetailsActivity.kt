package com.workers.nearwork.worker

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.workers.nearwork.R
import java.util.Locale

class RequestedWorkDetailsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var workPostId: String? = null

    // Views (Nullable to prevent crash if ID is missing)
    private var tvClientName: TextView? = null
    private var tvUserInitials: TextView? = null
    private var tvPostedTime: TextView? = null
    private var tvDescription: TextView? = null
    private var tvDuration: TextView? = null
    private var tvCategory: TextView? = null
    private var tvAddress: TextView? = null
    private var ivWorkImage: ImageView? = null
    private var tvBudget: TextView? = null
    private var etBidAmount: EditText? = null
    private var btnAccept: MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_requested_work_details)

        // 1. Initialize Views safely
        initViews()

        // 2. Get the Job ID passed from the previous screen
        workPostId = intent.getStringExtra("WORK_POST_ID")

        if (workPostId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Work ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 3. Load Details
        fetchWorkDetails(workPostId!!)

        // 4. Back Button
        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        // 5. Submit Bid Button
        // CRITICAL FIX: Ensure btnAccept is not null before setting listener
        btnAccept?.setOnClickListener {
            submitBid()
        } ?: run {
            Toast.makeText(this, "Error: Button not found in XML", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        // We use safe calls (?.) so if an ID is wrong, it won't crash the app immediately
        tvClientName = findViewById(R.id.tvClientName)
        tvUserInitials = findViewById(R.id.tvUserInitials)
        tvPostedTime = findViewById(R.id.tvPostedTime)
        tvDescription = findViewById(R.id.tvDescription)
        tvDuration = findViewById(R.id.tvDuration)
        tvCategory = findViewById(R.id.tvCategory)
        tvAddress = findViewById(R.id.tvAddress)
        ivWorkImage = findViewById(R.id.ivWorkImage)
        tvBudget = findViewById(R.id.tvBudgetRange)
        etBidAmount = findViewById(R.id.etBidAmount)

        // CHECK THIS ID IN YOUR XML: Is it @+id/btnAccept or @+id/btnSubmit?
        btnAccept = findViewById(R.id.btnAccept)
    }

    private fun fetchWorkDetails(postId: String) {
        db.collection("work_posts").document(postId)
            .get()
            .addOnSuccessListener { document ->
                // Check if activity is still valid before updating UI
                if (isFinishing || isDestroyed) return@addOnSuccessListener

                if (document.exists()) {
                    val category = document.getString("category") ?: "General"
                    val desc = document.getString("description") ?: ""
                    val address = document.getString("address") ?: "No address provided"
                    val budget = document.getString("budget") ?: "0"
                    val date = document.getString("date") ?: ""
                    val time = document.getString("time") ?: ""
                    val base64Image = document.getString("imageData")
                    val timestamp = document.getLong("timestamp") ?: System.currentTimeMillis()
                    val clientId = document.getString("clientId")

                    // Update UI Safely
                    tvCategory?.text = category.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                    tvDescription?.text = desc
                    tvAddress?.text = address
                    tvBudget?.text = "Client's Budget: $$budget"
                    tvDuration?.text = "$date at $time"

                    val timeAgo = DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                    tvPostedTime?.text = "Posted $timeAgo"

                    if (!base64Image.isNullOrEmpty()) {
                        decodeAndShowImage(base64Image)
                    } else {
                        ivWorkImage?.setImageResource(R.drawable.ic_image)
                    }

                    if (clientId != null) fetchClientProfile(clientId)
                } else {
                    Toast.makeText(this, "Job no longer exists", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchClientProfile(clientId: String) {
        db.collection("users").document(clientId).get()
            .addOnSuccessListener { document ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener

                if (document.exists()) {
                    val name = document.getString("fullName") ?: "Unknown Client"
                    tvClientName?.text = name

                    val initials = name.split(" ")
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .joinToString("").uppercase()
                    tvUserInitials?.text = initials
                }
            }
    }

    private fun decodeAndShowImage(base64String: String) {
        try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            ivWorkImage?.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun submitBid() {
        val postId = workPostId ?: return
        val workerId = auth.currentUser?.uid ?: return

        // 1. Validate Input
        val bidAmountStr = etBidAmount?.text.toString().trim()
        if (bidAmountStr.isEmpty()) {
            etBidAmount?.error = "Please enter your bid"
            return
        }

        // 2. DISABLE BUTTON to prevent Double Clicks (Crash Prevention)
        btnAccept?.isEnabled = false
        btnAccept?.text = "Sending..."

        val bidData = hashMapOf(
            "workerId" to workerId,
            "workPostId" to postId,
            "bidAmount" to bidAmountStr,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        val batch = db.batch()
        val jobBidRef = db.collection("work_posts").document(postId).collection("bids").document()
        val workerAppRef = db.collection("workers").document(workerId).collection("my_applications").document(postId)

        batch.set(jobBidRef, bidData)
        batch.set(workerAppRef, bidData)

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Bid Sent Successfully!", Toast.LENGTH_SHORT).show()
                // Delay closing slightly to ensure toast is seen
                finish()
            }
            .addOnFailureListener { e ->
                // Re-enable button if it failed
                btnAccept?.isEnabled = true
                btnAccept?.text = "Apply / Submit Bid"
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}