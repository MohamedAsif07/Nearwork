package com.workers.nearwork.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Bid(
    val workerId: String = "",
    val workPostId: String = "",
    // Changed to Any to prevent crashing if Firestore has a Number instead of String
    val bidAmount: Any = "0",
    val status: String = "pending",
    val timestamp: Long = 0
) {
    // Helper to safely get the amount as a Double for calculations
    fun getAmountAsDouble(): Double {
        return when (bidAmount) {
            is Number -> bidAmount.toDouble()
            is String -> bidAmount.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }
}