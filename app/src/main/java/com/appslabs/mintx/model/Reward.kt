package com.appslabs.mintx.model

data class Reward(
    var id: String = "",
    val name: String = "",
    val brand: String = "",
    val price: Int = 0,
    val logoUrl: String = "",  // URL for Firebase Storage or external image
    val colorHex: String = "#000000",
    val redemptionSteps: String = "1. Click Redeem\n2. Confirm your choice\n3. Copy the code from history",
    val instructions: String = "Use this code at the checkout page of the respective brand's website or app.",
    val verificationTimeline: String = "Instant",
    val isActive: Boolean = true,
    val createdAt: com.google.firebase.Timestamp? = null,
    val updatedAt: com.google.firebase.Timestamp? = null
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "", 0, "", "#000000")
}

