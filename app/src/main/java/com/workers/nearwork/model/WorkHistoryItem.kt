package com.workers.nearwork.model

import com.google.firebase.firestore.IgnoreExtraProperties

data class WorkHistoryItem(
    val category: String,
    var clientName: String, // Change 'val' to 'var' here
    val address: String,
    val description: String,
    val date: String,
    val time: String,
    val rating: Double,
    val clientFeedback: String
)