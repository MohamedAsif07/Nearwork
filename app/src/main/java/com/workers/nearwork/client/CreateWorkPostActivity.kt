package com.workers.nearwork.client

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.workers.nearwork.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class CreateWorkPostActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var etAddress: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var etBudget: EditText
    private lateinit var etDesc: EditText
    private lateinit var ivPreview: ImageView
    private lateinit var placeholderUI: LinearLayout

    private var encodedImage: String? = null
    private var selectedCategory: String = ""
    private var lat: Double = 0.0
    private var long: Double = 0.0

    // TODO: PUT YOUR SECRET KEY HERE
    private val GEMINI_API_KEY = "AIzaSyAUydgsEkk_Gd-zjKf8ZUv8l-yXAgaFCKc"

    private val geminiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) getCurrentLocation()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_work_post)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initViews()
        setupCategoryDropdown()
        setupDateTimePickers()
        setupImageUpload()
        setupLocation()
        setupPostButton()

        findViewById<View>(R.id.toolbar).setOnClickListener { finish() }
    }

    private fun initViews() {
        etAddress = findViewById(R.id.etAddress)
        etDate = findViewById(R.id.etDate)
        etTime = findViewById(R.id.etTime)
        etBudget = findViewById(R.id.etBudget)
        etDesc = findViewById(R.id.etDescription)
        ivPreview = findViewById(R.id.ivImagePreview)
        placeholderUI = findViewById(R.id.placeholderUI)
    }

    private fun setupImageUpload() {
        val selectImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                placeholderUI.visibility = View.GONE
                ivPreview.visibility = View.VISIBLE
                ivPreview.setImageURI(it)

                val b64 = encodeImageUri(it)
                if (b64 != null) {
                    encodedImage = b64
                    generateAIDescription(b64)
                }
            }
        }
        findViewById<FrameLayout>(R.id.layoutUpload).setOnClickListener { selectImage.launch("image/*") }
    }

    private fun generateAIDescription(base64Image: String) {
        if (base64Image.isEmpty()) return

        // Show loading state to the user
        etDesc.setText("AI is preparing a technical brief for the worker...")

        // This prompt is designed to help the worker come prepared
        val workerPrepPrompt = """
        Act as an expert manual labor consultant. Analyze this photo and provide:
        1. **Task Brief**: What exactly needs to be done?
        2. **Tool Checklist**: List specific tools or materials the worker should bring.
        3. **Preparation Suggestion**: One tip for the worker to prepare (e.g., 'Ensure the main power is off' or 'Clear the area under the sink').
        4. **Complexity**: Rate as Simple, Moderate, or Expert.
        
        Keep the tone professional and helpful for a technician.
    """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(
                    Part(text = workerPrepPrompt),
                    Part(inline_data = InlineData("image/jpeg", base64Image))
                ))
            )
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = geminiService.generateDescription(GEMINI_API_KEY, request)
                val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                withContext(Dispatchers.Main) {
                    if (!aiText.isNullOrBlank()) {
                        // Update the description box with the detailed brief
                        etDesc.setText(aiText.trim())
                    } else {
                        etDesc.setText("")
                        etDesc.setHint("AI could not analyze. Please describe the work manually.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    etDesc.setText("")
                    Toast.makeText(this@CreateWorkPostActivity, "AI Analysis Error. Please type details.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun encodeImageUri(uri: Uri): String? {
        var inputStream: InputStream? = null
        return try {
            inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream)
                val byteArray = outputStream.toByteArray()
                // NO_WRAP is the fix for Error 400
                Base64.encodeToString(byteArray, Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            null
        } finally {
            inputStream?.close()
        }
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf("Plumber", "Electrician", "Painter", "Carpenter", "Handyman", "Cleaner")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        findViewById<AutoCompleteTextView>(R.id.autoCompleteDomain).apply {
            setAdapter(adapter)
            setOnItemClickListener { parent, _, position, _ ->
                selectedCategory = parent.getItemAtPosition(position).toString().lowercase()
            }
        }
    }

    private fun setupDateTimePickers() {
        val calendar = Calendar.getInstance()
        etDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                calendar.set(year, month, day)
                etDate.setText(fmt.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        etTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                etTime.setText(fmt.format(calendar.time))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }
    }

    private fun setupLocation() {
        findViewById<ImageView>(R.id.btnGetCurrentLocation).setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    this.lat = location.latitude
                    this.long = location.longitude
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, long, 1)
                    if (!addresses.isNullOrEmpty()) etAddress.setText(addresses[0].getAddressLine(0))
                }
            }
        } catch (e: SecurityException) { }
    }

    private fun setupPostButton() {
        findViewById<Button>(R.id.btnPostWork).setOnClickListener {
            if (validateInputs()) uploadPost()
        }
    }

    private fun validateInputs(): Boolean {
        if (selectedCategory.isEmpty() || etAddress.text.isEmpty()) {
            Toast.makeText(this, "Complete all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun uploadPost() {
        val btn = findViewById<Button>(R.id.btnPostWork)
        btn.isEnabled = false
        btn.text = "Publishing..."

        val postId = db.collection("work_posts").document().id
        val postData = hashMapOf(
            "postId" to postId,
            "clientId" to auth.currentUser?.uid,
            "category" to selectedCategory,
            "address" to etAddress.text.toString(),
            "lat" to lat,
            "long" to long,
            "date" to etDate.text.toString(),
            "time" to etTime.text.toString(),
            "budget" to etBudget.text.toString(),
            "description" to etDesc.text.toString(),
            "imageData" to (encodedImage ?: ""),
            "status" to "open",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("work_posts").document(postId).set(postData)
            .addOnSuccessListener {
                Toast.makeText(this, "Posted Successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { btn.isEnabled = true }
    }
}