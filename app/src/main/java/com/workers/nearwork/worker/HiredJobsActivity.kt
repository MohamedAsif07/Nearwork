package com.workers.nearwork.worker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.workers.nearwork.R
import com.workers.nearwork.model.WorkPost

class HiredJobsActivity : AppCompatActivity() {

    private lateinit var rvJobs: RecyclerView
    private var adapter: WorkAdapter? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val workList = mutableListOf<WorkPost>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hired_jobs)

        // 1. Setup Views and Toolbar
        rvJobs = findViewById(R.id.rvHiredJobs)
        progressBar = findViewById(R.id.pbLoading)
        tvEmpty = findViewById(R.id.tvNoJobs)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // 2. Initialize Layout Manager
        rvJobs.layoutManager = LinearLayoutManager(this)

        // 3. Initialize Adapter with empty list immediately to prevent NullPointer crashes
        adapter = WorkAdapter(workList) { selectedWork ->
            val intent = Intent(this, AcceptedWorkActivity::class.java)
            intent.putExtra("WORK_POST_ID", selectedWork.postId)
            startActivity(intent)
        }
        rvJobs.adapter = adapter

        fetchMyHiredJobs()
    }

    private fun fetchMyHiredJobs() {
        val workerId = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE

        // This query matches the exact index requirement in your log
        db.collection("work_posts")
            .whereEqualTo("assignedWorkerId", workerId)
            .whereEqualTo("status", "assigned")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE
                workList.clear()

                for (doc in documents) {
                    val post = doc.toObject(WorkPost::class.java)
                    workList.add(post)
                }

                adapter?.notifyDataSetChanged()

                // Toggle empty state visibility
                if (workList.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    tvEmpty.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                // If the index isn't ready, this error message will appear
                Toast.makeText(this, "Fetching failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}