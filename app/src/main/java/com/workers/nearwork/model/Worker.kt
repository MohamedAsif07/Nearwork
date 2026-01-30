package com.workers.nearwork.model

data class Worker(
    var uid: String = "",
    var fullName: String = "",
    var email: String = "",
    var phone: String = "",

    // Make sure this matches how you save it: "category" or "domain"
    // based on your registration code, you used "category"
    var category: String = "",

    var experience: String = "0",
    var rating: Double = 0.0,

    var address: String = "",
    var doorNo: String = "",
    var pincode: String = "",

    var proofImageBase64: String = "",
    var userType: String = "worker",
    var isVerified: Boolean = false,

    var createdAt: Long = 0L
) {
    // Empty constructor is REQUIRED for Firebase
    constructor() : this("", "", "", "", "", "0", 0.0, "", "", "", "", "worker", false, 0L)
}