package com.workers.nearwork.worker

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
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
import com.workers.nearwork.model.WorkPost

class WorkerHomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FirestoreRecyclerAdapter<WorkPost, WorkViewHolder>
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_home)

        // 1. Initialize RecyclerView
        recyclerView = findViewById(R.id.rvWorkList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupFirestoreList()

        // ---------------------------------------------------------
        // ADDED: Logic for "Assigned Work" Button
        // ---------------------------------------------------------
        val btnAssignedWork = findViewById<MaterialButton>(R.id.btnAssignedWork)

        btnAssignedWork.setOnClickListener {
            // We navigate to AssignedWorkActivity (The List), NOT the Details page directly.
            // Navigating directly to RequestedWorkDetailsActivity here would crash
            // because we haven't selected a specific job ID yet.
            val intent = Intent(this, AssignedWorkActivity::class.java)
            startActivity(intent)
        }

        // Optional: History Button Logic
        val btnHistory = findViewById<MaterialButton>(R.id.btnHistory)
        btnHistory.setOnClickListener {
            Toast.makeText(this, "History feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFirestoreList() {
        val query = db.collection("work_posts")
            .whereEqualTo("status", "open")
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
                    // This handles clicking a SPECIFIC job card
                    val docId = snapshots.getSnapshot(position).id
                    val intent = Intent(this@WorkerHomeActivity, RequestedWorkDetailsActivity::class.java)
                    intent.putExtra("WORK_POST_ID", docId)
                    startActivity(intent)
                }
            }
        }
        recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    class WorkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvJobTitle)
        val tvDesc: TextView = itemView.findViewById(R.id.tvJobDesc)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivLocation)
    }
}