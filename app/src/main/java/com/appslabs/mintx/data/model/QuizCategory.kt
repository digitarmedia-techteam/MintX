package com.appslabs.mintx.data.model

data class SubCategory(
    val id: String,
    val name: String,
    var isSelected: Boolean = false
)

data class QuizCategory(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val icon: Int? = null,
    val imageUrl: String? = null,
    val subCategories: List<SubCategory> = emptyList(),
    var isExpanded: Boolean = false
)

