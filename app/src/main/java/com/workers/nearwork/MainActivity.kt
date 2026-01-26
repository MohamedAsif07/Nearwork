package com.workers.nearwork

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.workers.nearwork.auth.ClientLoginActivity
import com.workers.nearwork.auth.ClientRegisterActivity
import com.workers.nearwork.auth.WorkerLoginActivity
import com.workers.nearwork.worker.WorkerRegistrationActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val clientBtn = findViewById<Button>(R.id.btnClient)
        val workerBtn = findViewById<Button>(R.id.btnWorker)


        clientBtn.setOnClickListener {
            val intent = Intent(this, ClientLoginActivity::class.java)
            startActivity(intent)
        }
        workerBtn.setOnClickListener {
            val intent = Intent(this, WorkerLoginActivity::class.java)
            startActivity(intent)
        }
    }
}
