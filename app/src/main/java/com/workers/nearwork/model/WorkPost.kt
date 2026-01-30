package com.workers.nearwork.model

data class WorkPost(

    val date: String = "",
    val time: String = "",
    val priority: String = "Medium", // New field based on UI
    val notes: String = "",         // New field based on UI
    val postId: String = "",
    val assignedWorkerId: String = "", // MUST match exactly
    val clientId: String = "",
    val category: String = "",
    val description: String = "",
    val address: String = "", // Add this field
    val budget: String = "",  // Add this field to fix the error
    val imageData: String = "",
    val status: String = "",
    val timestamp: Long = 0,
    val finalPrice: String? = null,

)

