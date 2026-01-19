package com.digitar.mintx.data.model

import com.google.gson.annotations.SerializedName

data class QuizQuestion(
    @SerializedName("id") val id: Int,
    @SerializedName("question") val question: String,
    @SerializedName("description") val description: String?,
    @SerializedName("answers") val answers: Map<String, String?>,
    @SerializedName("multiple_correct_answers") val multipleCorrectAnswers: String,
    @SerializedName("correct_answers") val correctAnswers: Map<String, String>,
    @SerializedName("explanation") val explanation: String?,
    @SerializedName("category") val category: String,
    @SerializedName("difficulty") val difficulty: String
)

data class AnswerOptions(
    @SerializedName("answer_a") val a: String?,
    @SerializedName("answer_b") val b: String?,
    @SerializedName("answer_c") val c: String?,
    @SerializedName("answer_d") val d: String?,
    @SerializedName("answer_e") val e: String?,
    @SerializedName("answer_f") val f: String?
)
