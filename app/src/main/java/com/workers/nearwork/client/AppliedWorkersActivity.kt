package com.workers.nearwork.client

import android.content.Intent
import android.graphics.Color
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
import com.workers.nearwork.R

data class Bid(
    val workerId: String = "",
    val bidAmount: String = "",
    val status: String = "",
    val timestamp: Long = 0
)

class AppliedWorkersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private var adapter: FirestoreRecyclerAdapter<Bid, AppliedWorkerViewHolder>? = null
    private var workPostId: String? = null
    private var isJobFilled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applied_workers)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.rvAppliedWorkers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        workPostId = intent.getStringExtra("WORK_POST_ID")

        if (!workPostId.isNullOrEmpty()) {
            monitorJobStatus(workPostId!!)
            setupRecyclerView(workPostId!!)
        } else {
            Toast.makeText(this, "Error: Job ID missing.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun monitorJobStatus(postId: String) {
        db.collection("work_posts").document(postId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val status = snapshot.getString("status") ?: "open"
                isJobFilled = (status == "assigned" || status == "completed")
                adapter?.notifyDataSetChanged()
            }
    }

    private fun setupRecyclerView(postId: String) {
        val query = db.collection("work_posts").document(postId)
            .collection("bids").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Bid>()
            .setQuery(query, Bid::class.java).setLifecycleOwner(this).build()

        adapter = object : FirestoreRecyclerAdapter<Bid, AppliedWorkerViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppliedWorkerViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_applied_worker, parent, false)
                return AppliedWorkerViewHolder(view)
            }

            override fun onBindViewHolder(holder: AppliedWorkerViewHolder, position: Int, model: Bid) {
                holder.tvBidAmount.text = "Bid: $${model.bidAmount}"
                fetchWorkerName(model.workerId, holder.tvName)

                if (model.status == "accepted") {
                    holder.btnAccept.text = "Hired"
                    holder.btnAccept.isEnabled = false
                    holder.btnAccept.setBackgroundColor(Color.parseColor("#4CAF50"))
                } else {
                    if (isJobFilled) {
                        holder.btnAccept.text = "Closed"
                        holder.btnAccept.isEnabled = false
                        holder.btnAccept.setBackgroundColor(Color.LTGRAY)
                    } else {
                        holder.btnAccept.text = "Accept"
                        holder.btnAccept.isEnabled = true
                        holder.btnAccept.setBackgroundColor(Color.BLACK)
                    }
                }

                val bidDocId = snapshots.getSnapshot(position).id
                holder.btnAccept.setOnClickListener { acceptWorker(model, bidDocId) }
            }
        }
        recyclerView.adapter = adapter
    }

    private fun fetchWorkerName(workerId: String, textView: TextView) {
        db.collection("workers").document(workerId).get().addOnSuccessListener { doc ->
            textView.text = doc.getString("fullName") ?: "Unknown Worker"
        }
    }

    private fun acceptWorker(bid: Bid, bidDocId: String) {
        val postId = workPostId ?: return
        val batch = db.batch()

        val bidRef = db.collection("work_posts").document(postId).collection("bids").document(bidDocId)
        val postRef = db.collection("work_posts").document(postId)
        val workerAppRef = db.collection("workers").document(bid.workerId).collection("my_applications").document(postId)

        batch.update(bidRef, "status", "accepted")
        batch.update(postRef, "status", "assigned", "assignedWorkerId", bid.workerId, "finalPrice", bid.bidAmount)
        batch.update(workerAppRef, "status", "accepted")

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "Worker Hired Successfully!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, PendingWorkDetailsActivity::class.java)
            intent.putExtra("WORK_ID", postId)
            startActivity(intent)
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    class AppliedWorkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvWorkerName)
        val tvBidAmount: TextView = itemView.findViewById(R.id.tvBidAmount)
        val btnAccept: MaterialButton = itemView.findViewById(R.id.btnAcceptWorker)
    }
}