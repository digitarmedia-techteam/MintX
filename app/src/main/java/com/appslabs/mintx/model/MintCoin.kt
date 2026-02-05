package com.appslabs.mintx.model

data class MintCoin(
    val balance: Long,
    val history: List<MintTransaction>,
    val expiringSoon: Long
)

data class MintTransaction(
    val id: String,
    val amount: Long,
    val source: String, // "Quiz", "Prediction", "Partner Task"
    val timestamp: Long,
    val status: TransactionStatus
)

enum class TransactionStatus {
    PENDING, COMPLETED, EXPIRED
}
