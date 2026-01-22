package com.digitar.mintx.data.model

data class User(
    val firebaseUid: String = "",
    val phone: String = "",
    val name: String = "",
    val age: Int = 0,
    val countryCode: String = "",
    val categories: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
