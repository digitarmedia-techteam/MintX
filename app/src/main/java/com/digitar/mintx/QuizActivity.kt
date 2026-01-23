package com.digitar.mintx

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.digitar.mintx.data.model.QuizQuestion
import com.digitar.mintx.data.repository.QuizRepository
import com.digitar.mintx.databinding.ActivityQuizBinding
import com.digitar.mintx.ui.quiz.QuizCategoryBottomSheet
import com.digitar.mintx.ui.quiz.QuizViewModel
import java.util.Locale

class QuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizBinding

    private val viewModel: QuizViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuizViewModel(QuizRepository(this@QuizActivity)) as T
            }
        }
    }

    private var countDownTimer: CountDownTimer? = null
    private val TIMER_DURATION = 120000L // 2 minutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        window.statusBarColor = ContextCompat.getColor(this, R.color.mint_gold)

        setupClickListeners()
        setupBottomNavigation()
        observeViewModel()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.clSummary.visibility == View.VISIBLE) {
                    finish()
                } else {
                    showExitConfirmation()
                }
            }
        })

        val categories = intent.getStringArrayListExtra("categories")
        if (!categories.isNullOrEmpty()) {
            startQuizWithCategories(categories)
        } else {
             if (viewModel.questions.value.isNullOrEmpty()) {
                 // Default to Linux if no selection passed
                 startQuizWithCategories(listOf("Linux"))
             }
        }
    }

    private fun showQuizCategorySelector() {
        val bottomSheet = QuizCategoryBottomSheet.newInstance()
        bottomSheet.isCancelable = false
        
        bottomSheet.onQuizStarted = { categories ->
            startQuizWithCategories(categories)
        }
        
        bottomSheet.onQuit = {
            finish()
        }
        
        bottomSheet.show(supportFragmentManager, QuizCategoryBottomSheet.TAG)
    }

    private fun startQuizWithCategories(categories: List<String>) {
        animateQuizStart()
        viewModel.fetchQuestions(categories = categories)
    }

    private fun animateQuizStart() {
        val headerAnim = ObjectAnimator.ofFloat(
            binding.header.root, 
            "translationY", 
            -100f, 
            0f
        ).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
        }

        headerAnim.start()
    }
    
    private fun setupClickListeners() {
        binding.header.btnBack.setOnClickListener {
            showExitConfirmation()
        }
        


        binding.btnFinish.setOnClickListener {
            finish()
        }

        binding.btnPlayAgain.setOnClickListener {
            binding.clSummary.visibility = View.GONE
            viewModel.restartQuiz()
        }
        
        // Floating Controls Listeners
        binding.layoutFloating.cardHint.setOnClickListener {
            val isVisible = binding.layoutFloating.layoutHintOptions.visibility == View.VISIBLE
            binding.layoutFloating.layoutHintOptions.visibility = if (isVisible) View.GONE else View.VISIBLE
        }
        
        binding.layoutFloating.cardHintPoints.setOnClickListener {
            if (viewModel.deductPoints(10)) {
                binding.layoutFloating.layoutHintOptions.visibility = View.GONE
                val index = viewModel.currentIndex.value ?: 0
                val questions = viewModel.questions.value
                if (questions != null && index < questions.size) {
                    showCorrectAnswer(questions[index])
                    Toast.makeText(this, "Answer Revealed!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Not enough points (Need 10)!", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.layoutFloating.cardHintAd.setOnClickListener {
            binding.layoutFloating.layoutHintOptions.visibility = View.GONE
            Toast.makeText(this, "Ad functionality not implemented yet", Toast.LENGTH_SHORT).show()
            // Placeholder: Reveal answer anyway for testing
            // val index = viewModel.currentIndex.value ?: 0
            // val questions = viewModel.questions.value
            // if (questions != null && index < questions.size) {
            //     showCorrectAnswer(questions[index])
            // }
        }
    }

    private fun observeViewModel() {
        viewModel.questions.observe(this) { questions ->
            if (questions.isNotEmpty()) {
                if (binding.layoutSkeleton.root.visibility == View.VISIBLE) {
                    binding.layoutSkeleton.root.animate().alpha(0f).setDuration(300).withEndAction {
                        binding.layoutSkeleton.root.visibility = View.GONE
                    }
                    binding.clQuizContent.alpha = 0f
                    binding.clQuizContent.visibility = View.VISIBLE
                    binding.clQuizContent.animate().alpha(1f).setDuration(300).start()
                } else {
                    binding.clQuizContent.visibility = View.VISIBLE
                    binding.layoutSkeleton.root.visibility = View.GONE
                }
                updateUIForIndex(viewModel.currentIndex.value ?: 0)
            } else {
                binding.layoutSkeleton.root.visibility = View.VISIBLE
                binding.layoutSkeleton.root.alpha = 1f
                binding.clQuizContent.visibility = View.GONE
            }
        }

        viewModel.currentIndex.observe(this) { index ->
            updateUIForIndex(index)
        }

        viewModel.quizFinished.observe(this) { isFinished ->
            if (isFinished) {
                showSummary()
            }
        }

        viewModel.loading.observe(this) { isLoading ->
            // Toggle Skeleton and Content visibility
            if (isLoading) {
                binding.layoutSkeleton.root.visibility = View.VISIBLE
                binding.clQuizContent.visibility = View.GONE
            } else {
                binding.layoutSkeleton.root.visibility = View.GONE
                binding.clQuizContent.visibility = View.VISIBLE
            }
        }

        viewModel.currentScore.observe(this) { score ->
            binding.header.tvCurrentScore.text = "Pts: $score"
        }

        viewModel.scoreUpdateEvent.observe(this) { delta ->
            val animView = binding.header.tvScoreAnim
            animView.text = if (delta > 0) "+$delta" else "$delta"
            animView.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (delta > 0) R.color.mint_green else R.color.accent_red
                )
            )
            
            animView.visibility = View.VISIBLE
            animView.alpha = 0f
            animView.translationY = 20f

            animView.animate()
                .alpha(1f)
                .translationY(-20f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    animView.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .setStartDelay(200)
                        .withEndAction {
                            animView.visibility = View.GONE
                        }
                        .start()
                }
                .start()
        }

        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUIForIndex(index: Int) {
        val questions = viewModel.questions.value ?: return
        if (index >= questions.size) return

        val question = questions[index]
        
        startTimer()

        binding.header.tvQuizProgressText.text = "Question ${index + 1} of ${questions.size}"
        binding.header.tvQuestionLabel.text = "0${index + 1} Question"
        binding.header.pbQuizProgress.progress = ((index + 1) * 100) / questions.size
        
        binding.question.tvQuestionText.text = "Q${index + 1}. ${question.question}"
        
        binding.layoutFloating.layoutHintOptions.visibility = View.GONE

        // Update Category Label
        val categoryName = question.category
        binding.question.tvCategoryLabel.text = categoryName.uppercase()

        displayOptions(question, index)
    }

    private fun displayOptions(question: QuizQuestion, questionIndex: Int) {
        val optionContainers = listOf(binding.options.btnOption1, binding.options.btnOption2, binding.options.btnOption3, binding.options.btnOption4)
        val optionTexts = listOf(binding.options.tvOptionText1, binding.options.tvOptionText2, binding.options.tvOptionText3, binding.options.tvOptionText4)
        val selectedAnswer = viewModel.userAnswers.value?.get(questionIndex)

        optionContainers.forEach { 
            it.visibility = View.GONE
            it.isActivated = false
            it.isSelected = false
            it.isEnabled = true
        }

        val validAnswers = question.answers.filterValues { it != null }
        val answerKeys = validAnswers.keys.toList()

        for (i in 0 until minOf(optionContainers.size, answerKeys.size)) {
            val container = optionContainers[i]
            val textView = optionTexts[i]
            val key = answerKeys[i]
            val answerText = validAnswers[key]

            container.visibility = View.VISIBLE
            // Text View set only text, labels (A,B..) are static in layout
            textView.text = answerText

            if (selectedAnswer == key) {
                highlightSelection(container, key, question)
                optionContainers.forEach { it.isEnabled = false }
            }

            container.setOnClickListener {
                stopTimer()
                viewModel.selectAnswer(questionIndex, key)
                highlightSelection(container, key, question)
                optionContainers.forEach { it.isEnabled = false }
                
                binding.root.postDelayed({
                    if (!isFinishing && viewModel.currentIndex.value == questionIndex) {
                        viewModel.nextQuestion()
                    }
                }, 1000)
            }
        }
    }

    private fun highlightSelection(view: View, selectedKey: String, question: QuizQuestion) {
        val correctKey = "${selectedKey}_correct"
        val isCorrect = question.correctAnswers[correctKey] == "true"

        if (isCorrect) {
            view.isActivated = true
        } else {
            view.isSelected = true
            showCorrectAnswer(question)
        }
    }

    private fun showCorrectAnswer(question: QuizQuestion) {
        val optionContainers = listOf(binding.options.btnOption1, binding.options.btnOption2, binding.options.btnOption3, binding.options.btnOption4)
        val validAnswers = question.answers.filterValues { it != null }
        val answerKeys = validAnswers.keys.toList()

        val actualCorrectKey = question.correctAnswers.entries.find { it.value == "true" }?.key
        if (actualCorrectKey != null) {
            val baseKeyIdx = answerKeys.indexOf(actualCorrectKey.replace("_correct", ""))
            if (baseKeyIdx != -1 && baseKeyIdx < optionContainers.size) {
                optionContainers[baseKeyIdx].isActivated = true
            }
        }
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(TIMER_DURATION, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Smooth Progress Calculation
                val totalDuration = TIMER_DURATION.toFloat()
                val currentMillis = millisUntilFinished.toFloat()
                val progress = (currentMillis / totalDuration) * 100f
                
                // Color Transition Logic (Green -> Gold -> Red) based on %
                val colorRes = when {
                    progress > 60f -> R.color.mint_green
                    progress > 30f -> R.color.yellow
                    else -> R.color.accent_red
                }
                val color = ContextCompat.getColor(this@QuizActivity, colorRes)

                // Update Premium Border Timer
                binding.question.viewTimerBorder.setProgress(progress, color)
                
                // Update Analog Clock Timer (Floating)
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                
                binding.layoutFloating.viewFloatingTimer.setProgress(progress, color)
                binding.layoutFloating.viewFloatingTimer.setText(timeStr)
                
                // Breathing Animation for Floating Timer
                val scale = 1f + (0.05f * kotlin.math.sin(System.currentTimeMillis() / 200.0).toFloat())
                binding.layoutFloating.cardFloatingTimer.scaleX = scale
                binding.layoutFloating.cardFloatingTimer.scaleY = scale

                // Shake Hint Button if < 40 seconds
                if (millisUntilFinished <= 40000) {
                    val shake = ObjectAnimator.ofFloat(binding.layoutFloating.cardHint, "translationX", 0f, 10f, -10f, 10f, -10f, 0f)
                    shake.duration = 500
                    shake.start()
                }
            }

            override fun onFinish() {
                binding.question.viewTimerBorder.setProgress(0f, ContextCompat.getColor(this@QuizActivity, R.color.accent_red))
                // binding.timer.tvTimerText.text = "00:00"
                Toast.makeText(this@QuizActivity, "Time's up!", Toast.LENGTH_SHORT).show()
                viewModel.nextQuestion()
            }
        }.start()
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
    }

    private fun showSummary() {
        stopTimer()
        viewModel.saveQuizResults()
        val summary = viewModel.getQuizSummary()
        
        binding.clSummary.visibility = View.VISIBLE
        binding.clQuizContent.visibility = View.GONE
        
        binding.rowTotal.apply {
            tvLabel.text = "Total Questions"
            tvValue.text = summary.totalQuestions.toString()
        }
        binding.rowCorrect.apply {
            tvLabel.text = "Correct Answers"
            tvValue.text = summary.correctCount.toString()
            tvValue.setTextColor(ContextCompat.getColor(this@QuizActivity, R.color.mint_green))
        }
        binding.rowCorrectPoints.apply {
            tvLabel.text = "Points Earned"
            tvValue.text = "+${summary.correctPoints}"
            tvValue.setTextColor(ContextCompat.getColor(this@QuizActivity, R.color.mint_green))
        }
        binding.rowWrong.apply {
            tvLabel.text = "Wrong Answers"
            tvValue.text = summary.wrongCount.toString()
            tvValue.setTextColor(ContextCompat.getColor(this@QuizActivity, R.color.accent_red))
        }
        binding.rowNegativePoints.apply {
            tvLabel.text = "Points Deducted"
            tvValue.text = "-${summary.negativePoints}"
            tvValue.setTextColor(ContextCompat.getColor(this@QuizActivity, R.color.accent_red))
        }
        binding.rowSkipped.apply {
            tvLabel.text = "Skipped"
            tvValue.text = summary.skippedCount.toString()
        }
        
        binding.tvTotalPoints.text = "Total Points: ${summary.totalPoints}"

        val accuracy = if (summary.totalQuestions > 0) {
            ((summary.correctCount.toDouble() / summary.totalQuestions) * 100).toInt()
        } else 0
        
        binding.tvSummaryAccuracy.text = "$accuracy%"
        
        val indicatorColor = when {
            accuracy >= 70 -> R.color.mint_green
            accuracy >= 40 -> R.color.mint_gold
            else -> R.color.accent_red
        }
        
        binding.pbSummaryRing.setIndicatorColor(ContextCompat.getColor(this@QuizActivity, indicatorColor))
        binding.pbSummaryRing.setProgressCompat(accuracy, true)
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
    }

    override fun onResume() {
        super.onResume()
        if (binding.clSummary.visibility != View.VISIBLE && viewModel.questions.value?.isNotEmpty() == true) {
            startTimer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    private fun showExitConfirmation() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Quit Quiz?")
            .setMessage("Are you sure you want to quit? You will lose your current progress.")
            .setPositiveButton("Quit") { _, _ ->
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupBottomNavigation() {
        // Set 'Quiz' as active by default since we are in Quiz Screen
        binding.bottomNavigation.selectedItemId = R.id.navigation_quiz

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Explicitly navigate to Main Activity Home
                    val intent = android.content.Intent(this, MainActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                    intent.putExtra("NAV_ID", R.id.navigation_home)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_quiz -> {
                    // Already on Quiz
                    true
                }
                R.id.navigation_earn, R.id.navigation_wallet, R.id.navigation_prediction -> {
                    // Navigate to Main Activity and Switch Tab
                    val intent = android.content.Intent(this, MainActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                    intent.putExtra("NAV_ID", item.itemId)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
