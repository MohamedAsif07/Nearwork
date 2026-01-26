package com.workers.nearwork.model

data class WorkPost(
    val postId: String = "",
    val clientId: String = "",
    val category: String = "",
    val description: String = "",
    val imageData: String = "", // This stores the Base64 string
    val status: String = "",
    val timestamp: Long = 0
)