package com.workers.nearwork.worker

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView // Import CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.workers.nearwork.R

class AssignedWorkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_assigned_work)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.assignedwork)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // CORRECTED: Changed LinearLayout to CardView to match XML
        val btnAssinwork1 = findViewById<CardView>(R.id.btnAssignedWork1)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // Navigate to
        btnAssinwork1.setOnClickListener {
            val intent = Intent(this,HiredJobsActivity::class.java)
            // Note: In a real app, you'd pass a real ID from your data list here
            intent.putExtra("WORK_POST_ID", "sample_post_id")
            startActivity(intent)
        }

        // Back button logic
        btnBack.setOnClickListener {
            finish()
        }
    }
}