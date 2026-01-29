package com.digitar.mintx.data.model

import com.google.firebase.firestore.PropertyName

data class QuizQuestion(
    var id: String = "",
    var question: String = "",
    var description: String? = null,
    var answers: Map<String, String?> = emptyMap(),
    
    @get:PropertyName("multiple_correct_answers")
    @set:PropertyName("multiple_correct_answers")
    var multipleCorrectAnswers: String = "false",
    
    @get:PropertyName("correct_answers")
    @set:PropertyName("correct_answers")
    var correctAnswers: Map<String, String> = emptyMap(),
    
    var explanation: String? = null,
    var category: String = "General",
    var difficulty: String = "Easy"
)

data class AnswerOptions(
    @PropertyName("answer_a") val a: String?,
    @PropertyName("answer_b") val b: String?,
    @PropertyName("answer_c") val c: String?,
    @PropertyName("answer_d") val d: String?,
    @PropertyName("answer_e") val e: String?,
    @PropertyName("answer_f") val f: String?
)
