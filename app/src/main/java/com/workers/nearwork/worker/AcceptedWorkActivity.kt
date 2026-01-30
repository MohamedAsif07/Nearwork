package com.workers.nearwork.worker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.workers.nearwork.R

class AcceptedWorkActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var tvTaskName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvClientName: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var imgWork: ImageView
    private lateinit var btnShareLocation: MaterialButton
    private lateinit var btnMarkComplete: MaterialButton

    private var workPostId: String? = null
    private var clientLocationGeo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accepted_work)

        db = FirebaseFirestore.getInstance()
        initViews()

        workPostId = intent.getStringExtra("WORK_POST_ID")
        if (workPostId != null) {
            fetchJobDetails(workPostId!!)
        } else {
            Toast.makeText(this, "Error: Work ID not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupButtons()
    }

    private fun initViews() {
        val rowTask = findViewById<View>(R.id.rowTask)
        val rowLocation = findViewById<View>(R.id.rowLocation)
        val rowClient = findViewById<View>(R.id.rowClient)
        val rowDesc = findViewById<View>(R.id.rowDesc)

        rowTask.findViewById<TextView>(R.id.tvLabel).text = "Category"
        tvTaskName = rowTask.findViewById(R.id.tvValue)

        rowLocation.findViewById<TextView>(R.id.tvLabel).text = "Address"
        tvLocation = rowLocation.findViewById(R.id.tvValue)

        rowClient.findViewById<TextView>(R.id.tvLabel).text = "Client Name"
        tvClientName = rowClient.findViewById(R.id.tvValue)

        rowDesc.findViewById<TextView>(R.id.tvLabel).text = "Job Description"
        tvDescription = rowDesc.findViewById(R.id.tvValue)

        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)
        imgWork = findViewById(R.id.imgWork)
        btnShareLocation = findViewById(R.id.btnShareLocation)
        btnMarkComplete = findViewById(R.id.btnMarkComplete)

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
    }

    private fun fetchJobDetails(postId: String) {
        db.collection("work_posts").document(postId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    clientLocationGeo = document.getString("address")
                    tvTaskName.text = document.getString("category")?.uppercase()
                    tvLocation.text = clientLocationGeo
                    tvDescription.text = document.getString("description")
                    tvDate.text = document.getString("date")
                    tvTime.text = document.getString("time")

                    val base64Image = document.getString("imageData")
                    if (!base64Image.isNullOrEmpty()) {
                        try {
                            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                            Glide.with(this).asBitmap().load(imageBytes).into(imgWork)
                        } catch (e: Exception) { e.printStackTrace() }
                    }

                    document.getString("clientId")?.let { fetchClientInfo(it) }
                }
            }
    }

    private fun fetchClientInfo(clientId: String) {
        db.collection("users").document(clientId).get()
            .addOnSuccessListener { document ->
                tvClientName.text = document.getString("fullName") ?: "Unknown Client"
            }
    }

    private fun setupButtons() {
        btnShareLocation.setOnClickListener {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val geoPoint = "${location.latitude},${location.longitude}"

                        workPostId?.let { id ->
                            db.collection("work_posts").document(id)
                                .update("workerLiveLocation", geoPoint)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Location shared with Client!", Toast.LENGTH_SHORT).show()

                                    // Open Maps for the worker to find the client
                                    val intentUri = Uri.parse("google.navigation:q=${Uri.encode(clientLocationGeo)}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    startActivity(mapIntent)
                                }
                        }
                    } else {
                        Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        btnMarkComplete.setOnClickListener {
            workPostId?.let { id ->
                db.collection("work_posts").document(id)
                    .update("status", "completed")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Job Completed!", Toast.LENGTH_SHORT).show()
                        btnMarkComplete.isEnabled = false
                        btnMarkComplete.text = "Finished"
                    }
            }
        }
    }
}