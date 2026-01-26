package com.workers.nearwork.worker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.workers.nearwork.R
import com.workers.nearwork.client.CreateWorkPostActivity

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

        val btnPostWork = findViewById<LinearLayout>(R.id.reqwork)

        // 2. Set Click Listener to navigate to CreateWorkPostActivity
        btnPostWork.setOnClickListener {
            val intent = Intent(this, RequestedWorkDetailsActivity::class.java)
            startActivity(intent)
        }
    }
}