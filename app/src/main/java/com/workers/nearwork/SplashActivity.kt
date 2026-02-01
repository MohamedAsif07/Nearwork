package com.workers.nearwork

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.workers.nearwork.worker.WorkerHomeActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Simple delay to show the Lottie animation
        Handler(Looper.getMainLooper()).postDelayed({
            // Go directly to WorkerHomeActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)
    }
}