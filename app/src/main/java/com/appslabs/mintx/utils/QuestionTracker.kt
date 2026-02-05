package com.appslabs.mintx.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manages question visibility based on user's answer history.
 * 
 * Rules:
 * - Correctly answered questions are never shown again
 * - Incorrectly answered questions are shown only after 50 other questions have been attempted
 */
class QuestionTracker(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("question_tracker", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_CORRECT_QUESTIONS = "correct_questions"
        private const val KEY_WRONG_QUESTIONS = "wrong_questions"
        private const val KEY_QUESTION_COUNTER = "question_counter"
        private const val RETRY_THRESHOLD = 50
    }
    
    data class WrongQuestionEntry(
        val questionId: String,
        val counterAtWrongAnswer: Int
    )
    
    /**
     * Mark a question as correctly answered (never show again)
     */
    fun markQuestionCorrect(questionId: String) {
        val correctSet = getCorrectQuestions().toMutableSet()
        correctSet.add(questionId)
        saveCorrectQuestions(correctSet)
        
        // Remove from wrong questions if it was there
        val wrongList = getWrongQuestions().toMutableList()
        wrongList.removeAll { it.questionId == questionId }
        saveWrongQuestions(wrongList)
    }
    
    /**
     * Mark a question as incorrectly answered
     */
    fun markQuestionWrong(questionId: String) {
        val currentCounter = getCurrentCounter()
        val wrongList = getWrongQuestions().toMutableList()
        
        // Check if already in wrong list
        if (wrongList.none { it.questionId == questionId }) {
            wrongList.add(WrongQuestionEntry(questionId, currentCounter))
            saveWrongQuestions(wrongList)
        }
    }
    
    /**
     * Increment the counter when a question is attempted
     */
    fun incrementCounter() {
        val current = getCurrentCounter()
        prefs.edit().putInt(KEY_QUESTION_COUNTER, current + 1).apply()
    }
    
    /**
     * Get current question counter
     */
    private fun getCurrentCounter(): Int {
        return prefs.getInt(KEY_QUESTION_COUNTER, 0)
    }
    
    /**
     * Filter out questions that should not be shown
     */
    fun filterAvailableQuestions(allQuestions: List<String>): List<String> {
        val correctSet = getCorrectQuestions()
        val wrongList = getWrongQuestions()
        val currentCounter = getCurrentCounter()
        
        // Remove correctly answered questions
        val filtered = allQuestions.filter { questionId ->
            questionId !in correctSet
        }
        
        // Remove wrong questions that haven't met the retry threshold yet
        return filtered.filter { questionId ->
            val wrongEntry = wrongList.find { it.questionId == questionId }
            if (wrongEntry != null) {
                // Only show if at least 50 questions have been attempted since this was answered wrong
                (currentCounter - wrongEntry.counterAtWrongAnswer) >= RETRY_THRESHOLD
            } else {
                true // Not in wrong list, show it
            }
        }
    }
    
    /**
     * Check if a specific question is available to show
     */
    fun isQuestionAvailable(questionId: String): Boolean {
        val correctSet = getCorrectQuestions()
        
        // Never show if correctly answered
        if (questionId in correctSet) return false
        
        val wrongList = getWrongQuestions()
        val currentCounter = getCurrentCounter()
        
        // Check wrong question threshold
        val wrongEntry = wrongList.find { it.questionId == questionId }
        return if (wrongEntry != null) {
            (currentCounter - wrongEntry.counterAtWrongAnswer) >= RETRY_THRESHOLD
        } else {
            true
        }
    }
    
    // === Private Helper Methods ===
    
    private fun getCorrectQuestions(): Set<String> {
        val json = prefs.getString(KEY_CORRECT_QUESTIONS, null) ?: return emptySet()
        val type = object : TypeToken<Set<String>>() {}.type
        return gson.fromJson(json, type)
    }
    
    private fun saveCorrectQuestions(set: Set<String>) {
        val json = gson.toJson(set)
        prefs.edit().putString(KEY_CORRECT_QUESTIONS, json).apply()
    }
    
    private fun getWrongQuestions(): List<WrongQuestionEntry> {
        val json = prefs.getString(KEY_WRONG_QUESTIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<WrongQuestionEntry>>() {}.type
        return gson.fromJson(json, type)
    }
    
    private fun saveWrongQuestions(list: List<WrongQuestionEntry>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_WRONG_QUESTIONS, json).apply()
    }
    
    /**
     * Get statistics for debugging
     */
    fun getStats(): String {
        return """
            Total Questions Attempted: ${getCurrentCounter()}
            Correctly Answered (Never Show): ${getCorrectQuestions().size}
            Wrong Answers Tracked: ${getWrongQuestions().size}
        """.trimIndent()
    }
    
    /**
     * Clear all tracking data (for testing or reset)
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

