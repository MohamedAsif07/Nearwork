package com.workers.nearwork.client

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.workers.nearwork.R

class PendingWorkDetailsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var tvWorkerName: TextView
    private lateinit var tvWorkerExp: TextView
    private lateinit var tvWorkerPhone: TextView
    private lateinit var tvTaskName: TextView
    private lateinit var tvPriority: TextView
    private lateinit var tvNotes: TextView
    private lateinit var etTime: EditText
    private lateinit var etDate: EditText

    private var liveLocation: String? = null // To store worker's GPS
    private var jobAddress: String? = null    // Fallback address

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pending_work_details)

        initViews()

        val workId = intent.getStringExtra("WORK_ID")
        if (workId != null) {
            loadWorkDetails(workId)
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun initViews() {
        tvWorkerName = findViewById(R.id.tvWorkerName)
        tvWorkerExp = findViewById(R.id.tvWorkerExp)
        tvWorkerPhone = findViewById(R.id.tvWorkerPhone)
        tvTaskName = findViewById(R.id.tvTaskName)
        tvPriority = findViewById(R.id.tvPriorityValue)
        tvNotes = findViewById(R.id.tvNotesContent)
        etTime = findViewById(R.id.etArrivalTime)
        etDate = findViewById(R.id.etArrivalDate)
    }

    private fun loadWorkDetails(workId: String) {
        // Use addSnapshotListener for "Perfection" - UI updates instantly when worker shares location
        db.collection("work_posts").document(workId)
            .addSnapshotListener { doc, error ->
                if (doc != null && doc.exists()) {
                    tvTaskName.text = doc.getString("category")?.uppercase() ?: "Service"
                    tvNotes.text = doc.getString("description") ?: "No notes."
                    etTime.setText(doc.getString("time"))
                    etDate.setText(doc.getString("date"))
                    tvPriority.text = doc.getString("priority") ?: "Medium"

                    jobAddress = doc.getString("address")
                    liveLocation = doc.getString("workerLiveLocation") // REAL-TIME GPS

                    val workerId = doc.getString("assignedWorkerId")
                    if (workerId != null) {
                        fetchWorkerInfo(workerId)
                    }
                }
            }
    }

    private fun fetchWorkerInfo(workerId: String) {
        db.collection("workers").document(workerId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("fullName") ?: "Professional"
                    val phone = doc.getString("phone") ?: ""
                    val exp = doc.getString("experience") ?: "0"

                    tvWorkerName.text = name
                    tvWorkerExp.text = "$exp Years Experience"
                    tvWorkerPhone.text = phone

                    setupButtons(phone)
                }
            }
    }

    private fun setupButtons(phone: String) {
        // TRACK LOCATION BUTTON
        findViewById<Button>(R.id.btnTrackLocation).setOnClickListener {
            // Logic: If worker shared GPS, use it. Otherwise, use job address.
            val target = if (!liveLocation.isNullOrEmpty()) liveLocation else jobAddress

            if (!target.isNullOrEmpty()) {
                val mapUri = Uri.parse("google.navigation:q=$target")
                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                mapIntent.setPackage("com.google.android.apps.maps")

                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    // Fallback if Google Maps app is not installed
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$target"))
                    startActivity(browserIntent)
                }
            } else {
                Toast.makeText(this, "Location not available yet", Toast.LENGTH_SHORT).show()
            }
        }

        // CALL BUTTON
        findViewById<Button>(R.id.btnCall).setOnClickListener {
            if (phone.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            }
        }
    }
}