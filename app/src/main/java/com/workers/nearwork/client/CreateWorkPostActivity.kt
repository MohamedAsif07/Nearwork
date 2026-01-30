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
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.workers.nearwork.R
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

// ... (imports remain the same)

class CreateWorkPostActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Views
    private lateinit var etAddress: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var etBudget: EditText
    private lateinit var etDesc: EditText
    private lateinit var ivPreview: ImageView
    private lateinit var placeholderUI: LinearLayout

    // Variables for data - initialized to 0.0
    private var encodedImage: String? = null
    private var selectedCategory: String = ""
    private var lat: Double = 0.0
    private var long: Double = 0.0

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) getCurrentLocation()
            else Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
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
            } else {
                getCurrentLocation()
            }
        }
    }

    private fun getCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // CRITICAL: Update the global variables here
                    this.lat = location.latitude
                    this.long = location.longitude

                    val geocoder = Geocoder(this, Locale.getDefault())
                    try {
                        val addresses = geocoder.getFromLocation(lat, long, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addressLine = addresses[0].getAddressLine(0)
                            etAddress.setText(addressLine)
                        }
                    } catch (e: Exception) {
                        etAddress.setText("Lat: $lat, Long: $long")
                    }
                    Toast.makeText(this, "Location Updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Location not found. Ensure GPS is ON.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission Error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupImageUpload() {
        val selectImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                placeholderUI.visibility = View.GONE
                ivPreview.visibility = View.VISIBLE
                ivPreview.setImageURI(it)
                encodedImage = encodeImageUri(it)
            }
        }
        findViewById<FrameLayout>(R.id.layoutUpload).setOnClickListener { selectImage.launch("image/*") }
    }

    private fun setupPostButton() {
        findViewById<Button>(R.id.btnPostWork).setOnClickListener {
            if (validateInputs()) uploadPost()
        }
    }

    private fun validateInputs(): Boolean {
        if (selectedCategory.isEmpty()) { Toast.makeText(this, "Select a category", Toast.LENGTH_SHORT).show(); return false }
        if (etAddress.text.isEmpty()) { etAddress.error = "Address required"; return false }
        // Recommended: Check if location was actually fetched
        if (lat == 0.0 && long == 0.0) {
            Toast.makeText(this, "Please tap the location icon to fetch coordinates", Toast.LENGTH_SHORT).show()
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
            "lat" to lat, // This now contains the fetched latitude
            "long" to long, // This now contains the fetched longitude
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
                Toast.makeText(this, "Work Posted Successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                btn.isEnabled = true
                btn.text = "Post Work Now"
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun encodeImageUri(uri: Uri): String {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
}