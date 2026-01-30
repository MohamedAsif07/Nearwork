package com.workers.nearwork.client

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.workers.nearwork.model.Worker

import java.util.Locale

class ClientDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvRecTitle: TextView
    private var workerAdapter: FirestoreRecyclerAdapter<Worker, WorkerViewHolder>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_dashboard)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize Views
        recyclerView = findViewById(R.id.rvWorkers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Make sure your XML has this ID for the title (e.g., "Recommended Workers")
        tvRecTitle = findViewById(R.id.tvRecommendationTitle)

        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        // Run logic every time the activity starts (e.g. after posting a job)
        filterWorkersByLastPost()
    }

    override fun onStop() {
        super.onStop()
        workerAdapter?.stopListening()
    }

    private fun filterWorkersByLastPost() {
        val currentUserId = auth.currentUser?.uid ?: return

        // 1. Find the LAST job this user posted
        db.collection("work_posts")
            .whereEqualTo("clientId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING) // Newest first
            .limit(1) // We only need the top 1
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // CASE A: User HAS posted a job before
                    val lastJobCategory = documents.documents[0].getString("category") ?: ""

                    if (lastJobCategory.isNotEmpty()) {
                        // Update the Title
                        tvRecTitle.text = "Recommended ${lastJobCategory.capitalize()}s"

                        // STRICT FILTER: Show ONLY workers of this category
                        loadSpecificWorkers(lastJobCategory)
                    }
                } else {
                    // CASE B: User has NEVER posted a job -> Show everyone
                    tvRecTitle.text = "All Available Workers"
                    loadAllWorkers()
                }
            }
            .addOnFailureListener {
                // If query fails (e.g. missing index), fallback safely
                tvRecTitle.text = "All Workers"
                loadAllWorkers()
            }
    }

    private fun loadSpecificWorkers(category: String) {
        // Query: "workers" where "category" matches (lowercase)
        val query = db.collection("workers")
            .whereEqualTo("category", category.lowercase())

        startAdapter(query)
    }

    private fun loadAllWorkers() {
        // Fallback Query: Show everyone
        val query = db.collection("workers")
        startAdapter(query)
    }

    private fun startAdapter(query: Query) {
        // Stop old adapter to avoid conflicts
        workerAdapter?.stopListening()

        val options = FirestoreRecyclerOptions.Builder<Worker>()
            .setQuery(query, Worker::class.java)
            .build()

        workerAdapter = object : FirestoreRecyclerAdapter<Worker, WorkerViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_worker_card, parent, false)
                return WorkerViewHolder(view)
            }

            override fun onBindViewHolder(holder: WorkerViewHolder, position: Int, model: Worker) {
                // 1. Bind Data
                holder.tvName.text = model.fullName
                holder.tvDomain.text = model.category.capitalize()
                holder.tvExp.text = "${model.experience} Yrs Exp"

                // Optional: Set Rating (static or from DB)
                holder.tvRating.text = "4.8"

                // 2. Handle "Hire" Button Click
                holder.btnHire.setOnClickListener {
                    // Open the Details Activity
                    val intent = Intent(this@ClientDashboardActivity, WorkerDetailsActivity::class.java)

                    // Pass all worker data to the next screen
                    intent.putExtra("WORKER_ID", model.uid)
                    intent.putExtra("WORKER_NAME", model.fullName)
                    intent.putExtra("WORKER_CATEGORY", model.category.capitalize())
                    intent.putExtra("WORKER_EXP", model.experience)
                    intent.putExtra("WORKER_EMAIL", model.email)
                    intent.putExtra("WORKER_PHONE", model.phone)
                    intent.putExtra("WORKER_ADDRESS", model.address)

                    startActivity(intent)
                }
            }

            override fun onDataChanged() {
                // Optional: Handle empty state here if needed
                if (itemCount == 0) {
                    // e.g., show "No workers found" text
                }
            }
        }

        recyclerView.adapter = workerAdapter
        workerAdapter?.startListening()
    }


    private fun setupButtons() {
        // Button to go to Create Work Post
        findViewById<Button>(R.id.btnPostWork).setOnClickListener {
            startActivity(Intent(this, CreateWorkPostActivity::class.java))
        }

        // Button to go specifically to DashboardActivity
        val btnDashboard = findViewById<Button>(R.id.btnViewDashboard)
        btnDashboard.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            // This ensures you don't keep opening new versions of the same screen
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        // Ensure the variable name 'btnViewHistory' matches what you use below
        val btnViewHistory = findViewById<Button>(R.id.btnViewHistory)

        btnViewHistory.setOnClickListener {
            try {

                val intent = Intent(this, WorkHistoryActivity::class.java)
                startActivity(intent)
                // This ensures you don't keep opening new versions of the same screen
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            } catch (e: Exception) {
                // This will print the ACTUAL reason if it fails to open
                android.util.Log.e("NAV_ERROR", "Error opening History: ${e.message}")
                Toast.makeText(this, "Could not open History", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Helper to Capitalize Strings (plumber -> Plumber)
    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }


    // View Holder Class
    class WorkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvWorkerName)
        val tvDomain: TextView = itemView.findViewById(R.id.tvWorkerDomain)
        val tvExp: TextView = itemView.findViewById(R.id.tvWorkerExp)
        val tvRating: TextView = itemView.findViewById(R.id.tvWorkerRating) // Ensure this ID exists in item_worker_card
        val btnHire: Button = itemView.findViewById(R.id.btnHire)
    }
}

