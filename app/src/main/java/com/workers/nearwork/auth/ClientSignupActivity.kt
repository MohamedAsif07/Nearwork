package com.workers.nearwork.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.workers.nearwork.R

class ClientSignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_signup)

        auth = FirebaseAuth.getInstance()

        findViewById<Button>(R.id.btnNext).setOnClickListener {
            signup()
        }
    }

    private fun signup() {
        val email = findViewById<EditText>(R.id.etEmail).text.toString()
        val pass = findViewById<EditText>(R.id.etPassword).text.toString()
        val cPass = findViewById<EditText>(R.id.etConfirmPassword).text.toString()

        if (email.isEmpty() || pass.isEmpty() || cPass.isEmpty()) {
            toast("Fill all fields")
            return
        }

        if (pass != cPass) {
            toast("Passwords do not match")
            return
        }

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                startActivity(Intent(this, ClientRegisterActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                if (it.message!!.contains("already")) {
                    toast("User already registered, please login")
                    startActivity(Intent(this, ClientLoginActivity::class.java))
                    finish()
                } else {
                    toast(it.message!!)
                }
            }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
