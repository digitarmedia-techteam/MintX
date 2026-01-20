package com.digitar.mintx.ui.quiz

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitar.mintx.data.model.QuizCategory
import com.digitar.mintx.data.model.QuizQuestion
import com.digitar.mintx.data.repository.QuizRepository
import kotlinx.coroutines.launch

class QuizViewModel(private val repository: QuizRepository) : ViewModel() {

    private val _categories = MutableLiveData<List<QuizCategory>>()
    val categories: LiveData<List<QuizCategory>> = _categories

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _questions = MutableLiveData<List<QuizQuestion>>()
    val questions: LiveData<List<QuizQuestion>> = _questions

    // Quiz State
    private val _currentIndex = MutableLiveData(0)
    val currentIndex: LiveData<Int> = _currentIndex

    // Map of Index -> AnswerKey
    private val _userAnswers = MutableLiveData<MutableMap<Int, String>>(mutableMapOf())
    val userAnswers: LiveData<MutableMap<Int, String>> = _userAnswers

    // Quiz Result State
    private val _quizFinished = MutableLiveData<Boolean>(false)
    val quizFinished: LiveData<Boolean> = _quizFinished

    fun fetchCategories() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val result = repository.getCategories()
                _categories.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch categories"
            } finally {
                _loading.value = false
            }
        }
    }

    fun fetchQuestions(categories: List<String> = emptyList()) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            try {
                // Mapping user sub-categories to valid quizapi.io categories/tags
                // Valid categories: Linux, DevOps, Networking, Programming, Cloud, Docker, Kubernetes
                val apiCategories = categories.map { name ->
                    when (name.lowercase()) {
                        "coding", "software", "programming" -> "Code"
                        "linux & os", "linux", "operating systems" -> "Linux"
                        "networking" -> "Networking"
                        "devops" -> "DevOps"
                        "cloud" -> "Cloud"
                        "docker" -> "Docker"
                        "kubernetes" -> "Kubernetes"
                        else -> null // Will return random tech questions
                    }
                }.distinct()

                val allQuestions = mutableListOf<QuizQuestion>()
                
                if (apiCategories.isEmpty() || (apiCategories.size == 1 && apiCategories[0] == null)) {
                    // Fetch random if nothing specific or only unknown categories selected
                    val result = repository.getQuestions(null)
                    if (result != null) allQuestions.addAll(result)
                } else {
                    // Fetch for each mapped category to ensure we get a mix
                    apiCategories.forEach { cat ->
                        val result = repository.getQuestions(cat)
                        if (result != null) allQuestions.addAll(result)
                    }
                }

                if (allQuestions.isNotEmpty()) {
                    // Shuffle to mix if from multiple categories
                    _questions.value = allQuestions.shuffled().take(10)
                    _currentIndex.value = 0
                    _userAnswers.value = mutableMapOf()
                    _quizFinished.value = false
                } else {
                    _error.value = "Failed to load questions. Please check your API key and connection."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch questions"
            } finally {
                _loading.value = false
            }
        }
    }

    fun selectAnswer(questionIndex: Int, answerKey: String) {
        val currentAnswers = _userAnswers.value ?: mutableMapOf()
        currentAnswers[questionIndex] = answerKey
        _userAnswers.value = currentAnswers
    }

    fun nextQuestion() {
        val current = _currentIndex.value ?: 0
        val total = _questions.value?.size ?: 0
        if (current + 1 < total) {
            _currentIndex.value = current + 1
        } else {
            _quizFinished.value = true
        }
    }

    fun prevQuestion() {
        val current = _currentIndex.value ?: 0
        if (current > 0) {
            _currentIndex.value = current - 1
        }
    }

    fun getQuizSummary(): QuizSummary {
        val questions = _questions.value ?: emptyList()
        val answers = _userAnswers.value ?: emptyMap()
        
        var correctCount = 0
        var wrongCount = 0
        var skippedCount = 0
        
        questions.forEachIndexed { index, question ->
            val userChar = answers[index]?.replace("answer_", "") ?: ""
            if (userChar.isEmpty()) {
                skippedCount++
            } else {
                val correctKey = "answer_${userChar}_correct"
                if (question.correctAnswers[correctKey] == "true") {
                    correctCount++
                } else {
                    wrongCount++
                }
            }
        }
        
        return QuizSummary(
            totalQuestions = questions.size,
            correctCount = correctCount,
            wrongCount = wrongCount,
            skippedCount = skippedCount,
            totalPoints = correctCount * 2
        )
    }

    data class QuizSummary(
        val totalQuestions: Int,
        val correctCount: Int,
        val wrongCount: Int,
        val skippedCount: Int,
        val totalPoints: Int
    )
}
