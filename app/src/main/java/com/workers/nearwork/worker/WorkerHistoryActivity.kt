package com.workers.nearwork.worker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.workers.nearwork.R
import com.workers.nearwork.model.WorkHistoryItem
import java.util.Locale

class WorkerHistoryActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: HistoryAdapter
    private val historyList = mutableListOf<WorkHistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_history)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initUI()
        loadWorkerHistory()
    }

    private fun initUI() {
        val rvHistory = findViewById<RecyclerView>(R.id.rvWorkHistory)
        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(historyList)
        rvHistory.adapter = adapter

        findViewById<View>(R.id.statTotal)?.findViewById<TextView>(R.id.tvStatLabel)?.text = "Total Jobs"
        findViewById<View>(R.id.statRating)?.findViewById<TextView>(R.id.tvStatLabel)?.text = "Avg Rating"
        findViewById<View>(R.id.statSuccess)?.findViewById<TextView>(R.id.tvStatLabel)?.text = "Success"
        findViewById<View>(R.id.statSuccess)?.findViewById<TextView>(R.id.tvStatValue)?.text = "98%"

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
    }

    private fun loadWorkerHistory() {
        val workerId = auth.currentUser?.uid ?: return

        db.collection("work_posts")
            .whereEqualTo("workerId", workerId)
            .whereEqualTo("status", "completed")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Query Error: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    historyList.clear()
                    var totalRating = 0.0

                    if (snapshots.isEmpty) {
                        adapter.notifyDataSetChanged()
                        updateSummary(0, 0.0)
                        return@addSnapshotListener
                    }

                    for (doc in snapshots) {
                        val clientId = doc.getString("clientId")
                        val rating = doc.getDouble("rating") ?: 5.0
                        totalRating += rating

                        // Create the item with a placeholder name initially
                        val item = WorkHistoryItem(
                            category = doc.getString("category") ?: "Job",
                            clientName = "Loading...", // Placeholder
                            address = doc.getString("address") ?: "No Address",
                            description = doc.getString("description") ?: "No Description",
                            date = doc.getString("date") ?: "",
                            time = doc.getString("time") ?: "",
                            rating = rating,
                            clientFeedback = doc.getString("clientFeedback") ?: "Completed"
                        )
                        historyList.add(item)
                        val currentIndex = historyList.size - 1

                        // Fetch actual name from 'users' collection
                        if (clientId != null) {
                            db.collection("users").document(clientId).get()
                                .addOnSuccessListener { userDoc ->
                                    if (userDoc.exists()) {
                                        // Update the item in the list with the real name
                                        historyList[currentIndex].clientName = userDoc.getString("name") ?: "Client"
                                        adapter.notifyItemChanged(currentIndex)
                                    }
                                }
                        }
                    }
                    adapter.notifyDataSetChanged()
                    updateSummary(historyList.size, totalRating)
                }
            }
    }

    private fun updateSummary(count: Int, totalRating: Double) {
        val totalBox = findViewById<View>(R.id.statTotal)
        totalBox?.findViewById<TextView>(R.id.tvStatValue)?.text = count.toString()

        if (count > 0) {
            val avg = totalRating / count
            val ratingBox = findViewById<View>(R.id.statRating)
            ratingBox?.findViewById<TextView>(R.id.tvStatValue)?.text = String.format(Locale.getDefault(), "%.1f", avg)
        } else {
            findViewById<View>(R.id.statRating)?.findViewById<TextView>(R.id.tvStatValue)?.text = "0.0"
        }
    }

    inner class HistoryAdapter(private val list: List<WorkHistoryItem>) :
        RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val tvClient: TextView = view.findViewById(R.id.tvClientName)
            val tvAddress: TextView = view.findViewById(R.id.tvHistoryAddress)
            val tvDescription: TextView = view.findViewById(R.id.tvHistoryDescription)
            val tvFeedback: TextView = view.findViewById(R.id.tvFeedback)
            val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
            val tvRatingValue: TextView = view.findViewById(R.id.tvRatingValue)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_worker_completed_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvCategory.text = item.category
            holder.tvClient.text = "Client: ${item.clientName}"
            holder.tvAddress.text = "Location: ${item.address}"
            holder.tvDescription.text = item.description
            holder.tvFeedback.text = "\"${item.clientFeedback}\""
            holder.ratingBar.rating = item.rating.toFloat()
            holder.tvRatingValue.text = item.rating.toString()
        }

        override fun getItemCount() = list.size
    }
}