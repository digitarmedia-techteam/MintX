package com.digitar.mintx.data.model

data class Transaction(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val amount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "credit", // "credit" or "debit"
    val status: String = "completed" // "completed", "pending", "failed"
)
