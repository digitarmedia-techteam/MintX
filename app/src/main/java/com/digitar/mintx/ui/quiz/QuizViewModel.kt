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

    // Total XP
    private val _totalXP = MutableLiveData<Long>(0)
    val totalXP: LiveData<Long> = _totalXP

    // Store current categories for restart
    private var currentCategories: List<String> = emptyList()

    init {
        fetchUserData()
    }

    private fun fetchUserData() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            val balance = repository.getUserBalance(uid)
            val xp = repository.getUserXP(uid)
            _mintBalance.value = balance
            _totalXP.value = xp
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

    fun startQuizWithUserPreferences() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                _loading.value = true
                val categories = repository.getUserCategories(uid)
                if (categories.isNotEmpty()) {
                    fetchQuestions(categories)
                } else {
                    // Fallback if no categories selected: Use default or handle accordingly
                    // For now, defaulting to 'Linux' as per previous logic, or maybe fetch questions from ANY category?
                    // User request: "Only the questions from the categories selected by the user should be shown"
                    // If none selected, we might want to guide them to selection or show random?
                    // I will stick to the previous fallback logic for safety but ideally trigger onboarding.
                     fetchQuestions(listOf("Linux")) 
                }
            }
        } else {
             fetchQuestions(listOf("Linux"))
        }
    }

    // ... (rest of fetchQuestions)

    fun restartQuiz() {
        // Reset state immediately to prevent UI race conditions
        _loading.value = true // Immediate visual feedback (Skeleton)
        _currentIndex.value = 0
        _quizFinished.value = false
        _currentScore.value = 0
        _userAnswers.value = mutableMapOf()
        
        fetchQuestions(currentCategories)
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
                    // Update Balance
                    repository.updateUserBalance(uid, newBalance)
                    
                    // Create Transaction for Debit (Hint Used)
                    val transaction = com.digitar.mintx.data.model.Transaction(
                        id = java.util.UUID.randomUUID().toString(),
                        title = "Hint Used",
                        description = "Points spent on in-game hint",
                        amount = amount.toDouble(),
                        timestamp = System.currentTimeMillis(),
                        type = "debit",
                        status = "completed"
                    )
                    repository.saveTransaction(uid, transaction)
                }
            }
            return true
        }
        return false
    }

    fun saveQuizResults() {
        val summary = getQuizSummary()
        if (summary.totalPoints != 0) {
            val currentBalance = _mintBalance.value ?: 0
            val currentXP = _totalXP.value ?: 0
            
            // Logic for Low Accuracy Penalty: Fixed at -1 if score is negative
            val finalPointsChange = if (summary.totalPoints < 0) -1 else summary.totalPoints
            
            // Prevent Negative Balance
            var newBalance = currentBalance + finalPointsChange
            if (newBalance < 0) newBalance = 0
            
            // Update XP (Only add accumulated points, never decrease below 0? Or just partial add?)
            // User Request: "Only the actual points earned should be added to the level progress (the same amount as Total Points Earned)."
            // Interpreting this as adding the exact Total Points.
            var newXP = currentXP + summary.totalPoints
            if (newXP < 0) newXP = 0

            _mintBalance.value = newBalance
            _totalXP.value = newXP
            
            // Sync with Firestore
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                viewModelScope.launch {
                    // Update Balance
                    repository.updateUserBalance(uid, newBalance)
                    // Update XP
                    repository.updateUserXP(uid, newXP)
                    // Update Solved Stats
                    repository.updateUserSolvedStats(uid, summary.correctEasy, summary.correctMedium, summary.correctHard)
                    
                    // Determine Transaction Details
                    val isCredit = finalPointsChange > 0
                    val absAmount = kotlin.math.abs(finalPointsChange)
                    val type = if (isCredit) "credit" else "debit"
                    val title = if (isCredit) "Quiz Earnings" else "Quiz Penalty"
                    val description = if (isCredit) "Reward for completing daily quiz" else "Points deducted for low accuracy"
                    
                    // Create and Save Transaction Record
                    val transaction = com.digitar.mintx.data.model.Transaction(
                        id = java.util.UUID.randomUUID().toString(),
                        title = title,
                        description = description,
                        amount = absAmount.toDouble(),
                        timestamp = System.currentTimeMillis(),
                        type = type,
                        status = "completed"
                    )
                    repository.saveTransaction(uid, transaction)
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
                // Pass categories directly to repository (Firestore)
                val result = repository.getQuestions(categories)
                
                if (result.isNotEmpty()) {
                    _questions.value = result.take(10) // Limit to 10 questions per session
                    _currentIndex.value = 0
                    _userAnswers.value = mutableMapOf()
                    _currentScore.value = 0
                    _quizFinished.value = false
                } else {
                    _error.value = "No questions found for the selected categories."
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
        
        android.util.Log.d("QuizDebug", "Selected: $answerKey, looking for: $correctKey")
        android.util.Log.d("QuizDebug", "Available Correct Keys: ${question.correctAnswers.keys}")
        android.util.Log.d("QuizDebug", "Is Correct? $isCorrect")
        
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
        
        var correctEasy = 0
        var correctMedium = 0
        var correctHard = 0
        
        questions.forEachIndexed { index, question ->
            val userChar = answers[index]?.replace("answer_", "") ?: ""
            if (userChar.isEmpty()) {
                skippedCount++
            } else {
                val correctKey = "answer_${userChar}_correct"
                if (question.correctAnswers[correctKey] == "true") {
                    correctCount++
                    // Count Difficulty
                    when (question.difficulty.lowercase()) {
                        "easy" -> correctEasy++
                        "medium" -> correctMedium++
                        "hard" -> correctHard++
                    }
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
            totalPoints = totalPoints,
            correctEasy = correctEasy,
            correctMedium = correctMedium,
            correctHard = correctHard
        )
    }

    data class QuizSummary(
        val totalQuestions: Int,
        val correctCount: Int,
        val wrongCount: Int,
        val skippedCount: Int,
        val correctPoints: Int,
        val negativePoints: Int,
        val totalPoints: Int,
        val correctEasy: Int = 0,
        val correctMedium: Int = 0,
        val correctHard: Int = 0
    )
}

