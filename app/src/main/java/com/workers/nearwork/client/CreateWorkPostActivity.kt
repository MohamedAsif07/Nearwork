package com.workers.nearwork.client

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.workers.nearwork.R
import java.io.ByteArrayOutputStream
import java.io.InputStream

class CreateWorkPostActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Class-level button to avoid unresolved reference errors
    private lateinit var btnPostWork: Button

    private var encodedImage: String? = null
    private var selectedCategory: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_work_post)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initializing views
        val autoCompleteDomain = findViewById<AutoCompleteTextView>(R.id.autoCompleteDomain)
        val etDesc = findViewById<EditText>(R.id.etDescription)
        val layoutUpload = findViewById<FrameLayout>(R.id.layoutUpload)
        val placeholderUI = findViewById<LinearLayout>(R.id.placeholderUI)
        val ivPreview = findViewById<ImageView>(R.id.ivImagePreview)
        btnPostWork = findViewById(R.id.btnPostWork)

        // 1. Setup Dropdown Menu
        val categories = arrayOf("Plumber", "Electrician", "Painter", "Carpenter", "Handyman", "Cleaner")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        autoCompleteDomain.setAdapter(adapter)

        autoCompleteDomain.setOnItemClickListener { parent, _, position, _ ->
            selectedCategory = parent.getItemAtPosition(position).toString().lowercase()
        }

        // 2. Image Selection
        val selectImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                placeholderUI.visibility = View.GONE
                ivPreview.visibility = View.VISIBLE
                ivPreview.setImageURI(it)
                encodedImage = encodeImageUri(it)
            }
        }

        layoutUpload.setOnClickListener { selectImage.launch("image/*") }

        // 3. Post Logic
        btnPostWork.setOnClickListener {
            val desc = etDesc.text.toString().trim()

            if (selectedCategory.isEmpty()) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (encodedImage == null) {
                Toast.makeText(this, "Please upload an image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (desc.length < 10) {
                etDesc.error = "Description too short"
                return@setOnClickListener
            }

            uploadToFirebase(selectedCategory, desc, encodedImage!!)
        }
    }

    private fun uploadToFirebase(category: String, desc: String, imageStr: String) {
        // Now btnPostWork is recognized correctly
        btnPostWork.isEnabled = false
        btnPostWork.text = "Uploading..."

        val postId = db.collection("work_posts").document().id
        val postData = hashMapOf(
            "postId" to postId,
            "clientId" to auth.currentUser?.uid,
            "category" to category,
            "description" to desc,
            "imageData" to imageStr,
            "status" to "open",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("work_posts").document(postId)
            .set(postData)
            .addOnSuccessListener {
                Toast.makeText(this, "Job posted successfully!", Toast.LENGTH_SHORT).show()
                finish() // Returns to Dashboard
            }
            .addOnFailureListener {
                btnPostWork.isEnabled = true
                btnPostWork.text = "Post Work"
                Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun encodeImageUri(uri: Uri): String {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        // Strong compression to stay under 1MB Firestore limit
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
}