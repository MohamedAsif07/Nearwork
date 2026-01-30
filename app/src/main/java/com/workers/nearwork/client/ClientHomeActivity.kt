package com.workers.nearwork.client

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.workers.nearwork.R
import com.workers.nearwork.model.WorkPost

class ClientHomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddPost: FloatingActionButton
    private var adapter: FirestoreRecyclerAdapter<WorkPost, ClientJobViewHolder>? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_home)

        recyclerView = findViewById(R.id.rvMyPostedJobs)
        fabAddPost = findViewById(R.id.fabAddPost)

        recyclerView.layoutManager = LinearLayoutManager(this)

        setupMyPostedJobsList()

        fabAddPost.setOnClickListener {
            startActivity(Intent(this, CreateWorkPostActivity::class.java))
        }
    }

    private fun setupMyPostedJobsList() {
        val currentUser = auth.currentUser ?: return

        // Fetch jobs for this client, ordered by time
        val query = db.collection("work_posts")
            .whereEqualTo("clientId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<WorkPost>()
            .setQuery(query, WorkPost::class.java)
            .setLifecycleOwner(this) // Automatically handles start/stop listening
            .build()

        adapter = object : FirestoreRecyclerAdapter<WorkPost, ClientJobViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientJobViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_work_card, parent, false)
                return ClientJobViewHolder(view)
            }

            override fun onBindViewHolder(holder: ClientJobViewHolder, position: Int, model: WorkPost) {
                holder.tvTitle.text = model.category.replaceFirstChar { it.uppercase() }
                holder.tvDesc.text = model.description

                // FIX: Use bindingAdapterPosition to avoid Inconsistency crashes
                holder.itemView.setOnClickListener {
                    val currentPos = holder.bindingAdapterPosition
                    if (currentPos != RecyclerView.NO_POSITION) {
                        val docId = snapshots.getSnapshot(currentPos).id

                        val intent = Intent(this@ClientHomeActivity, AppliedWorkersActivity::class.java)
                        intent.putExtra("WORK_POST_ID", docId)
                        startActivity(intent)
                    }
                }
            }
        }

        recyclerView.adapter = adapter
    }

    // Explicitly manage listener lifecycle to prevent memory leaks and crashes
    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    class ClientJobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvJobTitle)
        val tvDesc: TextView = itemView.findViewById(R.id.tvJobDesc)
    }
}