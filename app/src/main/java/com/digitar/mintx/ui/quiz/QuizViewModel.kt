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
    
    // Live Score
    private val _currentScore = MutableLiveData(0)
    val currentScore: LiveData<Int> = _currentScore
    
    // Event for score update animation (Delta)
    private val _scoreUpdateEvent = com.digitar.mintx.utils.SingleLiveEvent<Int>()
    val scoreUpdateEvent: LiveData<Int> = _scoreUpdateEvent

    // Mint Balance
    private val _mintBalance = MutableLiveData<Long>(0)
    val mintBalance: LiveData<Long> = _mintBalance

    // Store current categories for restart
    private var currentCategories: List<String> = emptyList()

    init {
        fetchMintBalance()
    }

    private fun fetchMintBalance() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            val balance = repository.getUserBalance(uid)
            _mintBalance.value = balance
        }
    }

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

    // ... (rest of fetchQuestions)

    fun restartQuiz() {
        fetchQuestions(currentCategories)
        _currentScore.value = 0
    }

    fun deductPoints(amount: Int): Boolean {
        val currentBalance = _mintBalance.value ?: 0
        if (currentBalance >= amount) {
            val newBalance = currentBalance - amount
            _mintBalance.value = newBalance
            
            // Sync with Firestore
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                viewModelScope.launch {
                    repository.updateUserBalance(uid, newBalance)
                }
            }
            return true
        }
        return false
    }

    fun saveQuizResults() {
        val summary = getQuizSummary()
        if (summary.totalPoints > 0) {
            val currentBalance = _mintBalance.value ?: 0
            val newBalance = currentBalance + summary.totalPoints
            _mintBalance.value = newBalance
            
            // Sync with Firestore
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                viewModelScope.launch {
                    repository.updateUserBalance(uid, newBalance)
                }
            }
        }
    }





    fun fetchQuestions(categories: List<String> = emptyList()) {
        this.currentCategories = categories
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

                // Mapping logic (unused but kept for reference)
                // val apiCategories = ...
                
                val categoryQuestionsMap = mutableMapOf<String, MutableList<QuizQuestion>>()
                
                // Always fetch ALL questions (Repository is forced to return all)
                val result = repository.getQuestions(null)
                
                if (result != null) {
                    // Group questions by their defined category
                    result.forEach { q ->
                        val cat = q.category
                        val key = cat.trim()
                        if (!categoryQuestionsMap.containsKey(key)) {
                            categoryQuestionsMap[key] = mutableListOf()
                        }
                        categoryQuestionsMap[key]?.add(q)
                    }
                }

                // Create rotated list: Strict 2 questions per category per rotation cycle
                val rotatedList = mutableListOf<QuizQuestion>()
                
                // Get all available categories (e.g. Science, Tech, History...)
                val availableCategories = categoryQuestionsMap.keys.toMutableList()
                
                // Shuffle categories to start with a random one each time
                availableCategories.shuffle()
                
                // Shuffle questions WITHIN each category to vary content
                categoryQuestionsMap.values.forEach { it.shuffle() }

                while (rotatedList.size < 10 && availableCategories.isNotEmpty()) {
                    val iterator = availableCategories.iterator()
                    while (iterator.hasNext()) {
                        val cat = iterator.next()
                        val questions = categoryQuestionsMap[cat]
                        
                        if (questions.isNullOrEmpty()) {
                            iterator.remove() // Exhausted this category
                            continue
                        }
                        
                        // Take 2 questions
                        val batchSize = 2
                        val batch = questions.take(batchSize)
                        rotatedList.addAll(batch)
                        
                        // Remove used questions
                        questions.removeAll(batch)
                        
                        if (rotatedList.size >= 10) break
                    }
                }

                if (rotatedList.isNotEmpty()) {
                    _questions.value = rotatedList.take(10) // Ensure max 10
                    _currentIndex.value = 0
                    _userAnswers.value = mutableMapOf()
                    _currentScore.value = 0
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
        val questions = _questions.value ?: return
        if (questionIndex >= questions.size) return

        val currentAnswers = _userAnswers.value ?: mutableMapOf()
        
        // Check if already answered to prevent double scoring
        if (currentAnswers.containsKey(questionIndex)) return

        currentAnswers[questionIndex] = answerKey
        _userAnswers.value = currentAnswers
        
        // Update Score
        val question = questions[questionIndex]
        val correctKey = "${answerKey}_correct"
        val isCorrect = question.correctAnswers[correctKey] == "true"
        
        val currentScoreVal = _currentScore.value ?: 0
        if (isCorrect) {
            _currentScore.value = currentScoreVal + 2
            _scoreUpdateEvent.value = 2
        } else {
            _currentScore.value = currentScoreVal - 1
            _scoreUpdateEvent.value = -1
        }
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
        
        val correctPoints = correctCount * 2
        val negativePoints = wrongCount * 1 // 1 point deduction for wrong answer
        val totalPoints = correctPoints - negativePoints
        
        return QuizSummary(
            totalQuestions = questions.size,
            correctCount = correctCount,
            wrongCount = wrongCount,
            skippedCount = skippedCount,
            correctPoints = correctPoints,
            negativePoints = negativePoints,
            totalPoints = totalPoints
        )
    }

    data class QuizSummary(
        val totalQuestions: Int,
        val correctCount: Int,
        val wrongCount: Int,
        val skippedCount: Int,
        val correctPoints: Int,
        val negativePoints: Int,
        val totalPoints: Int
    )
}
