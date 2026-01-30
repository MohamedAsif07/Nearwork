package com.workers.nearwork.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.workers.nearwork.R
import com.workers.nearwork.model.WorkHistory


class WorkHistoryActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: HistoryAdapter
    private val historyList = mutableListOf<WorkHistory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work_history)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val rvHistory = findViewById<RecyclerView>(R.id.rvWorkHistory)
        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(historyList)
        rvHistory.adapter = adapter

        loadHistory()
    }

    private fun loadHistory() {
        val currentUserId = auth.currentUser?.uid ?: return

        // We query work_posts where status is completed AND the user is the worker
        // OR the client (depending on whose history you want to show).
        // Assuming this is for the CLIENT to see their past orders:
        db.collection("work_posts")
            .whereEqualTo("clientId", currentUserId)
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val item = doc.toObject(WorkHistory::class.java)
                    historyList.add(item)
                }
                adapter.notifyDataSetChanged()
            }
    }

    // Simple Adapter Class
    inner class HistoryAdapter(private val list: List<WorkHistory>) :
        RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val category: TextView = view.findViewById(R.id.tvHistoryCategory)
            val date: TextView = view.findViewById(R.id.tvHistoryDate)
            val status: TextView = view.findViewById(R.id.tvHistoryStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_work_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.category.text = item.category
            holder.date.text = "Date: ${item.date}"
            holder.status.text = item.status.uppercase()
        }

        override fun getItemCount() = list.size
    }
}