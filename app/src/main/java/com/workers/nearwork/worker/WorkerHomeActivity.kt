package com.workers.nearwork.worker

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.workers.nearwork.R
import com.workers.nearwork.model.WorkPost
import com.workers.nearwork.utils.WrapContentLinearLayoutManager

class WorkerHomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private var adapter: FirestoreRecyclerAdapter<WorkPost, WorkViewHolder>? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_home)

        // 1. Initialize UI Elements
        recyclerView = findViewById(R.id.rvWorkList)
        recyclerView.layoutManager = WrapContentLinearLayoutManager(this)

        val btnHistoryActivity = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHistory)
        val btnAssignedWork = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAssignedWork)

        // 2. Navigation Listeners
        btnHistoryActivity.setOnClickListener {
            startActivity(Intent(this, WorkerHistoryActivity::class.java))
        }

        btnAssignedWork.setOnClickListener {
            startActivity(Intent(this, AssignedWorkActivity::class.java))
        }

        // 3. Load User Profile and Jobs
        fetchWorkerCategoryAndLoadJobs()
    }

    private fun fetchWorkerCategoryAndLoadJobs() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("workers").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // --- GET AND SHOW WORKER NAME ---
                        val workerName = document.getString("fullName")
                        val tvUserName = findViewById<TextView>(R.id.tvUserName)
                        tvUserName.text = workerName ?: "Worker"

                        // --- GET CATEGORY AND LOAD LIST ---
                        val myCategory = document.getString("category")
                        if (!myCategory.isNullOrEmpty()) {
                            setupFirestoreList(myCategory)
                        } else {
                            Toast.makeText(this, "No category found in your profile.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Worker profile not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error fetching profile: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupFirestoreList(category: String) {
        // Query: Get 'open' jobs matching the worker's category
        val query = db.collection("work_posts")
            .whereEqualTo("status", "open")
            .whereEqualTo("category", category.lowercase()) // Ensuring case sensitivity matches registration
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<WorkPost>()
            .setQuery(query, WorkPost::class.java)
            .build()

        adapter = object : FirestoreRecyclerAdapter<WorkPost, WorkViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_work_card, parent, false)
                return WorkViewHolder(view)
            }

            override fun onBindViewHolder(holder: WorkViewHolder, position: Int, model: WorkPost) {
                holder.tvTitle.text = model.category.replaceFirstChar { it.uppercase() }
                holder.tvDesc.text = model.description

                // Decode Image if exists
                if (model.imageData.isNotEmpty()) {
                    try {
                        val decodedBytes = Base64.decode(model.imageData, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        holder.ivThumbnail.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        holder.ivThumbnail.setImageResource(R.drawable.ic_image)
                    }
                }

                holder.itemView.setOnClickListener {
                    val docId = snapshots.getSnapshot(position).id
                    val intent = Intent(this@WorkerHomeActivity, RequestedWorkDetailsActivity::class.java)
                    intent.putExtra("WORK_POST_ID", docId)
                    startActivity(intent)
                }
            }
        }

        recyclerView.adapter = adapter
        adapter?.startListening()
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    class WorkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvJobTitle)
        val tvDesc: TextView = itemView.findViewById(R.id.tvJobDesc)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivLocation)
    }
}