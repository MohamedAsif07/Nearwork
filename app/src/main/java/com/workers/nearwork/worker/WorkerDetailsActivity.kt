package com.workers.nearwork.client

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.workers.nearwork.R

class WorkerDetailsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var workerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_details)

        db = FirebaseFirestore.getInstance()

        // 1. Get Data from Intent
        workerId = intent.getStringExtra("WORKER_ID")
        val name = intent.getStringExtra("WORKER_NAME") ?: "Unknown"
        val category = intent.getStringExtra("WORKER_CATEGORY") ?: ""
        val exp = intent.getStringExtra("WORKER_EXP") ?: "0"
        val email = intent.getStringExtra("WORKER_EMAIL") ?: "No email"
        val phone = intent.getStringExtra("WORKER_PHONE") ?: "No phone"
        val location = intent.getStringExtra("WORKER_ADDRESS") ?: "No address"

        // 2. Populate UI
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvDetailName).text = name
        findViewById<TextView>(R.id.tvDetailExperience).text = "$exp years experience"
        findViewById<TextView>(R.id.tvDetailCategory).text = "Specialized in $category"
        findViewById<TextView>(R.id.tvDetailEmail).text = email
        findViewById<TextView>(R.id.tvDetailPhone).text = phone
        findViewById<TextView>(R.id.tvDetailLocation).text = location

        // 3. Button Logic
        val btnDecline = findViewById<MaterialButton>(R.id.btnDecline)
        val btnAccept = findViewById<MaterialButton>(R.id.btnAccept)

        btnDecline.setOnClickListener {
            finish() // Just close the page
        }

        btnAccept.setOnClickListener {
            hireWorker(name)
        }
    }

    private fun hireWorker(workerName: String) {
        // You can save this request to Firestore so the Worker sees it
        // Or simply show success for now

        val btnAccept = findViewById<MaterialButton>(R.id.btnAccept)
        btnAccept.isEnabled = false
        btnAccept.text = "Processing..."

        // Example: Saving a "Hire Request" to Firestore (Optional but recommended)
        /*
        val request = hashMapOf(
            "workerId" to workerId,
            "clientId" to FirebaseAuth.getInstance().currentUser?.uid,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("hire_requests").add(request)...
        */

        // For now, just simulating success and closing:
        Toast.makeText(this, "Request sent to $workerName!", Toast.LENGTH_LONG).show()

        // Go back to dashboard or show a "Success" screen
        finish()
    }
}