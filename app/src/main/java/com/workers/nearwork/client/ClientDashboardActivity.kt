package com.workers.nearwork.client

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.workers.nearwork.R

class ClientDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enables the layout to span across the status and navigation bars
        enableEdgeToEdge()
        setContentView(R.layout.activity_client_dashboard)

        // Handle window insets to prevent the UI from being hidden under system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Find the Post Work Button from activity_client_dashboard.xml
        val btnPostWork = findViewById<Button>(R.id.btnPostWork)

        // 2. Set Click Listener to navigate to CreateWorkPostActivity
        btnPostWork.setOnClickListener {
            val intent = Intent(this, CreateWorkPostActivity::class.java)
            startActivity(intent)
        }
    }
}