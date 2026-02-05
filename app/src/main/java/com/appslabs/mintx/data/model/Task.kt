package com.appslabs.mintx.data.model

data class Task(
    val id: String,
    val title: String,
    val reward: String,
    val status: String,
    val iconResId: Int,
    val instructions: List<String>,
    val howToClaim: String,
    var isExpanded: Boolean = false
)

