package com.workers.nearwork.model

data class WorkHistory(
    val category: String = "",
    val description: String = "",
    val date: String = "",
    val status: String = "",
    val clientName: String = "" // Optional: if you want to show who they worked for
)
