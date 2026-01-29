package com.digitar.mintx.data.model

import com.google.firebase.Timestamp
import java.util.Date

data class Transaction(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val timestamp: Any? = null, // Supports Long or Timestamp
    val type: String = "credit", // "credit" or "debit"
    val status: String = "completed" // "completed", "pending", "failed"
) {
    fun getTimestampLong(): Long {
        return when (timestamp) {
            is Long -> timestamp
            is Timestamp -> timestamp.toDate().time
            is Date -> timestamp.time
            else -> 0L
        }
    }
}
