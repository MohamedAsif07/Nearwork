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
import androidx.recyclerview.widget.LinearLayoutManager
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
    // Adapter is nullable because we wait for the category to load before creating it
    private var adapter: FirestoreRecyclerAdapter<WorkPost, WorkViewHolder>? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_home)

        // 1. Initialize RecyclerView
        recyclerView = findViewById(R.id.rvWorkList)
        // Use the new class you just created
        recyclerView.layoutManager = WrapContentLinearLayoutManager(this)

        // 2. We do NOT call setupFirestoreList() here immediately.
        // We must find out if the worker is a "Plumber" or "Electrician" first.
        fetchWorkerCategoryAndLoadJobs()
        val btnHistoryActivity = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHistory)

        btnHistoryActivity.setOnClickListener {
            // Navigate to the list of jobs where the worker is hired
            val intent = Intent(this, WorkerHistoryActivity::class.java)
            startActivity(intent)
        }
        val btnAssignedWork = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAssignedWork)

        btnAssignedWork.setOnClickListener {
            // Navigate to the list of jobs where the worker is hired
            val intent = Intent(this, AssignedWorkActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchWorkerCategoryAndLoadJobs() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Get the worker's profile to find their category
            db.collection("workers").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Get the category (e.g., "plumber")
                        // Ensure this matches the field name in your Registration Activity ("category")
                        val myCategory = document.getString("category")

                        if (!myCategory.isNullOrEmpty()) {
                            // Now that we know the category, load the specific jobs
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
        // 3. Query: Get 'open' jobs AND matching category
        val query = db.collection("work_posts")
            .whereEqualTo("status", "open")
            .whereEqualTo("category", category) // <--- THIS FILTERS THE LIST
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
        // Only listen if adapter is created
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