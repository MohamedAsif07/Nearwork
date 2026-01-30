package com.workers.nearwork.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.workers.nearwork.R

class DashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupSystemBars()
        setupNavigationButtons()
    }

    private fun setupSystemBars() {
        val mainView = findViewById<View>(R.id.main)
        mainView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }

    private fun setupNavigationButtons() {
        // --- BUTTON 1: VIEW APPLIED WORKERS (Applicants List) ---
        findViewById<View>(R.id.cardApplied)?.setOnClickListener {
            val currentUserId = auth.currentUser?.uid ?: return@setOnClickListener

            // Find the latest job you posted to see who applied
            db.collection("work_posts")
                .whereEqualTo("clientId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val lastPostId = documents.documents[0].id
                        val intent = Intent(this, ApplicantsListActivity::class.java)
                        intent.putExtra("WORK_POST_ID", lastPostId)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "You haven't posted any jobs yet", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // --- BUTTON 2: PENDING WORK DETAILS (Dynamic) ---
        // This finds the job that is currently "assigned" to a worker
        findViewById<LinearLayout>(R.id.btnpending)?.setOnClickListener {
            val currentUserId = auth.currentUser?.uid ?: return@setOnClickListener

            db.collection("work_posts")
                .whereEqualTo("clientId", currentUserId)
                .whereEqualTo("status", "assigned") // Look for jobs currently in progress
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val realId = documents.documents[0].id
                        val intent = Intent(this, PendingWorkDetailsActivity::class.java)
                        intent.putExtra("WORK_ID", realId)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "No active assigned jobs found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // --- BUTTON 3: GO TO CLIENT HOME (To post new work) ---
        // I noticed your XML uses ID 'post' inside the first card for navigation
        findViewById<LinearLayout>(R.id.post)?.setOnClickListener {
            startActivity(Intent(this, ClientHomeActivity::class.java))
        }
    }
}