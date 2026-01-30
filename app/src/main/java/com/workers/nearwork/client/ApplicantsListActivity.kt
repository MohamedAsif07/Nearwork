package com.workers.nearwork.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.workers.nearwork.R
import com.workers.nearwork.model.Bid

class ApplicantsListActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var adapter: FirestoreRecyclerAdapter<Bid, ApplicantViewHolder>? = null
    private lateinit var recyclerView: RecyclerView
    private var workPostId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applicants_list)

        // Get ID from Intent
        workPostId = intent.getStringExtra("WORK_POST_ID")

        if (workPostId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Work ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.rvApplicants)
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupFirestoreList()
    }

    private fun setupFirestoreList() {
        // Updated Query: Ensure path is work_posts -> {id} -> bids
        val query = db.collection("work_posts")
            .document(workPostId!!)
            .collection("bids")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Bid>()
            .setQuery(query, Bid::class.java)
            .build()

        adapter = object : FirestoreRecyclerAdapter<Bid, ApplicantViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicantViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_applicant_card, parent, false)
                return ApplicantViewHolder(view)
            }

            override fun onBindViewHolder(holder: ApplicantViewHolder, position: Int, model: Bid) {
                // 1. Show Bid Amount using the helper method from our model
                holder.tvBidAmount.text = "₹${model.bidAmount}"

                // 2. Fetch Worker Details from the 'workers' collection
                if (model.workerId.isNotEmpty()) {
                    db.collection("workers").document(model.workerId).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                holder.tvWorkerName.text = document.getString("fullName") ?: "Unknown"
                                holder.tvWorkerRole.text = document.getString("category")?.uppercase() ?: "WORKER"
                            }
                        }
                }

                // 3. Hire Button Logic
                holder.btnHire.setOnClickListener {
                    val price = model.getAmountAsDouble()
                    hireWorker(model.workerId, price)
                }
            }

            override fun onDataChanged() {
                // If there are no bids, notify the user so they don't see just a white screen
                if (itemCount == 0) {
                    Toast.makeText(this@ApplicantsListActivity, "No applicants yet", Toast.LENGTH_SHORT).show()
                }
            }
        }

        recyclerView.adapter = adapter
    }

    private fun hireWorker(workerId: String, agreedPrice: Double) {
        val updateData = hashMapOf<String, Any>(
            "status" to "accepted",
            "workerId" to workerId,
            "agreedPrice" to agreedPrice
        )

        db.collection("work_posts").document(workPostId!!)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(this, "Worker Hired Successfully!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to hire: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    class ApplicantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Ensure these IDs match your item_applicant_card.xml exactly
        val tvWorkerName: TextView = itemView.findViewById(R.id.tvWorkerName)
        val tvWorkerRole: TextView = itemView.findViewById(R.id.tvWorkerRole)
        val tvBidAmount: TextView = itemView.findViewById(R.id.tvBidAmount)
        val btnHire: MaterialButton = itemView.findViewById(R.id.btnHire)
    }
}