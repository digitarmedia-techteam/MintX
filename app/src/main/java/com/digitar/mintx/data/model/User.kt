package com.digitar.mintx.data.model

data class User(
    val firebaseUid: String = "",
    val phone: String = "",
    val name: String = "",
    val age: Int = 0,
    val countryCode: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val categories: List<String> = emptyList(),
    val mintBalance: Long = 0,
    val totalXP: Long = 0,
    val solvedEasy: Int = 0,
    val solvedMedium: Int = 0,
    val solvedHard: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val activityDates: List<Long> = emptyList(),
    val dailyStats: Map<String, Int> = emptyMap()
)
