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
        window.statusBarColor = ContextCompat.getColor(this, R.color.mint_gold)

        setupClickListeners()
        observeViewModel()

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
            finish()
        }
        


        binding.btnFinish.setOnClickListener {
            finish()
        }

        binding.btnPlayAgain.setOnClickListener {
            binding.clSummary.visibility = View.GONE
            viewModel.restartQuiz()
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
        
        // Update Category Label
        val categoryName = question.category
        binding.question.tvCategoryLabel.text = categoryName.uppercase()

        displayOptions(question, index)
    }

    private fun displayOptions(question: QuizQuestion, questionIndex: Int) {
        val options = listOf(binding.options.btnOption1, binding.options.btnOption2, binding.options.btnOption3, binding.options.btnOption4)
        val selectedAnswer = viewModel.userAnswers.value?.get(questionIndex)

        options.forEach { 
            it.visibility = View.GONE
            it.isActivated = false
            it.isSelected = false
            it.isEnabled = true
        }

        val validAnswers = question.answers.filterValues { it != null }
        val answerKeys = validAnswers.keys.toList()

        for (i in 0 until minOf(options.size, answerKeys.size)) {
            val view = options[i]
            val key = answerKeys[i]
            val answerText = validAnswers[key]

            view.visibility = View.VISIBLE
            val label = when(i) {
                0 -> "A"
                1 -> "B"
                2 -> "C"
                3 -> "D"
                else -> ""
            }
            view.text = "$label. $answerText"

            if (selectedAnswer == key) {
                highlightSelection(view, key, question)
                options.forEach { it.isEnabled = false }
            }

            view.setOnClickListener {
                stopTimer()
                viewModel.selectAnswer(questionIndex, key)
                highlightSelection(view, key, question)
                options.forEach { it.isEnabled = false }
                
                binding.root.postDelayed({
                    if (!isFinishing && viewModel.currentIndex.value == questionIndex) {
                        viewModel.nextQuestion()
                    }
                }, 1000)
            }
        }
    }

    private fun highlightSelection(view: TextView, selectedKey: String, question: QuizQuestion) {
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
        val options = listOf(binding.options.btnOption1, binding.options.btnOption2, binding.options.btnOption3, binding.options.btnOption4)
        val validAnswers = question.answers.filterValues { it != null }
        val answerKeys = validAnswers.keys.toList()

        val actualCorrectKey = question.correctAnswers.entries.find { it.value == "true" }?.key
        if (actualCorrectKey != null) {
            val baseKeyIdx = answerKeys.indexOf(actualCorrectKey.replace("_correct", ""))
            if (baseKeyIdx != -1 && baseKeyIdx < options.size) {
                options[baseKeyIdx].isActivated = true
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
                    progress > 30f -> R.color.mint_gold
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
}
