package com.appslabs.mintx.model

data class Redemption(
    var id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val rewardId: String = "",
    val rewardName: String = "",
    val rewardBrand: String = "",
    val rewardPrice: Int = 0,
    val rewardLogoUrl: String = "",
    val status: String = "pending", // pending, approved, rejected
    val requestedAt: com.google.firebase.Timestamp? = null,
    val processedAt: com.google.firebase.Timestamp? = null,
    val processedBy: String? = null,
    val redemptionCode: String? = null,
    val adminNotes: String? = null
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "", "", "", "", "", 0, "", "pending")
    
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"
    }
}

