package com.digitar.mintx.model

data class Reward(
    val id: String,
    val name: String,
    val brand: String,
    val price: Int,
    val brandLogo: Int,
    val colorHex: String,
    val redemptionSteps: String = "1. Click Redeem\n2. Confirm your choice\n3. Copy the code from history",
    val instructions: String = "Use this code at the checkout page of the respective brand's website or app.",
    val verificationTimeline: String = "Instant"
)
