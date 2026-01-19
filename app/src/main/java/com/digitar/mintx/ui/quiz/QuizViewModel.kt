package com.digitar.mintx.ui.quiz

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitar.mintx.data.model.QuizCategory
import com.digitar.mintx.data.model.QuizQuestion
import com.digitar.mintx.data.repository.QuizRepository
import com.digitar.mintx.ui.quiz.QuizCategoryBottomSheet.Companion.TAG
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


    fun fetchQuestions(category: String? = null) {
        viewModelScope.launch {
            Log.d(TAG, "fetchQuestions() called, category = $category")

            _loading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Calling repository.getQuestions()")

                val result = repository.getQuestions(category)

                Log.d(TAG, "Questions result = $result")

                if (result != null && result.isNotEmpty()) {
                    _questions.value = result
                    _currentIndex.value = 0
                    _userAnswers.value = mutableMapOf()
                    _quizFinished.value = false

                    Log.d(TAG, "Questions loaded successfully. Total = ${result.size}")
                } else {
                    _error.value = "Failed to load questions"
                    Log.e(TAG, "Question list is null or empty")
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch questions"
                Log.e(TAG, "Exception while fetching questions", e)

            } finally {
                _loading.value = false
                Log.d(TAG, "Loading finished")
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

    fun selectCategory(categoryName: String) {
        val currentList = _categories.value ?: return
        val newList = currentList.map {
            it.copy(isSelected = it.name == categoryName)
        }
        _categories.value = newList
    }

    fun getSelectedCategory(): QuizCategory? {
        return _categories.value?.find { it.isSelected }
    }
}
