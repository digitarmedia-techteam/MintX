package com.digitar.mintx

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.MotionEvent
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
import com.digitar.mintx.utils.AdManager.showRewardedAd
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt
import com.digitar.mintx.utils.LevelUtils

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
    private var isShowingAd = false // Track if ad is currently being shown
    private var isShowingHint = false // Track if hint is currently being revealed
    private var isShowingInterstitial = false // Track if interstitial ad is being shown
    private var isTogglingHint = false // Prevent multiple rapid clicks on hint button
    private var hintTapCount = 0 // Track triple-tap progress
    private var hasShownHintInstruction = false // Track if we've shown the hint instruction

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
                if (binding.clSummary.root.visibility == View.VISIBLE) {
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
                 animateQuizStart()
                 viewModel.startQuizWithUserPreferences()
             }
        }
        
        // Preload Rewarded Ad
        com.digitar.mintx.utils.AdManager.loadRewardedAd(this)
        
        // Preload Interstitial Ad
        com.digitar.mintx.utils.AdManager.loadInterstitialAd(this)
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
            binding.clQuizContent.header.root, 
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
        binding.clQuizContent.header.btnBack.setOnClickListener {
            showExitConfirmation()
        }
        

        binding.clSummary.btnFinish.setOnClickListener {
            finish()
        }

        binding.clSummary.btnPlayAgain.setOnClickListener {
            binding.clSummary.root.visibility = View.GONE
            viewModel.restartQuiz()
        }
        
        
        
        // Floating Controls Listeners - Hint button with drag and click
        var dX = 0f
        var dY = 0f
        var initialX = 0f
        var initialY = 0f
        var hasMoved = false
        
        binding.layoutFloating.cardHint.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    initialX = view.x
                    initialY = view.y
                    hasMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY
                    
                    // Check if moved significantly (more than 10px)
                    if (kotlin.math.abs(newX - initialX) > 10 || kotlin.math.abs(newY - initialY) > 10) {
                        hasMoved = true
                        view.animate()
                            .x(newX)
                            .y(newY)
                            .setDuration(0)
                            .start()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved) {
                        // It was a click, not a drag - implement triple-tap
                        view.performClick()
                        
                        hintTapCount++
                        
                        // Pulse animation on each tap with increasing intensity
                        val scale = 1f - (hintTapCount * 0.05f) // Gets smaller with each tap
                        view.animate()
                            .scaleX(scale)
                            .scaleY(scale)
                            .setDuration(80)
                            .withEndAction {
                                view.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(80)
                                    .start()
                            }
                            .start()
                        
                        when (hintTapCount) {
                            1 -> {
                                // First tap - show instruction only once per session
                                if (!hasShownHintInstruction) {
                                    Toast.makeText(this, "💡 Tap 3 times to open hints", Toast.LENGTH_SHORT).show()
                                    hasShownHintInstruction = true
                                }
                            }
                            2 -> {
                                // Second tap - just visual feedback (no toast)
                                // Shimmer effect to indicate progress
                                (view as? com.facebook.shimmer.ShimmerFrameLayout)?.startShimmer()
                            }
                            3 -> {
                                // Third tap - open hint options!
                                hintTapCount = 0 // Reset counter
                                (view as? com.facebook.shimmer.ShimmerFrameLayout)?.stopShimmer()
                                toggleHintOptions()
                                
                                // Success feedback - celebratory spin
                                view.animate()
                                    .rotationBy(360f)
                                    .setDuration(300)
                                    .start()
                            }
                        }
                        
                        // Reset counter after 1.5 seconds of inactivity
                        view.removeCallbacks(null)
                        view.postDelayed({
                            if (hintTapCount < 3) {
                                hintTapCount = 0
                                (view as? com.facebook.shimmer.ShimmerFrameLayout)?.stopShimmer()
                            }
                        }, 1500)
                    }
                    true
                }
                else -> false
            }
        }

        
        binding.layoutFloating.cardHintPoints.setOnClickListener {
            if (viewModel.deductPoints(10)) {
                closeHintOptions()
                handleHintUsed()
            } else {
                Toast.makeText(this, "Not enough points (Need 10)!", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.layoutFloating.cardHintAd.setOnClickListener {
            closeHintOptions()
            
            // Set flag to prevent quiz termination
            isShowingAd = true
            
            // Show Rewarded Ad
            showRewardedAd(
                this,
                onRewardEarned = {
                    // User watched the ad, reveal the answer
                    // Ad is already fully dismissed at this point
                    isShowingAd = false
                    
                    // Small delay for smooth transition
                    binding.root.postDelayed({
                        if (!isFinishing) {
                            handleHintUsed()
                        }
                    }, 300)
                },
                onAdClosed = {
                    // Ad closed (whether completed or not)
                    isShowingAd = false
                    // If user didn't complete, they don't get the hint
                }
            )
        }
    }

    private fun handleHintUsed() {
        val index = viewModel.currentIndex.value ?: 0
        val questions = viewModel.questions.value
        
        if (questions != null && index < questions.size) {
            isShowingHint = true // Set flag to prevent timer restart
            stopTimer()
            disableOptions()
            
            // Show with blinking gold border (isHintReveal = true)
            showCorrectAnswer(questions[index], isHintReveal = true)
            Toast.makeText(this, "Answer Revealed!", Toast.LENGTH_SHORT).show()
            
            // Auto-advance after 3 seconds
            binding.root.postDelayed({
                if (!isFinishing && viewModel.currentIndex.value == index) {
                    isShowingHint = false // Reset flag before moving to next question
                    viewModel.nextQuestion()
                }
            }, 3000)
        }
    }

    private fun observeViewModel() {
        viewModel.questions.observe(this) { questions ->
            if (questions.isNotEmpty()) {
                // If skeleton is currently visible, it means this is a fresh load (or restart)
                if (binding.layoutSkeleton.root.visibility == View.VISIBLE && binding.layoutSkeleton.root.alpha > 0f) {
                    // First time loading - show interstitial ad before quiz starts
                    isShowingInterstitial = true
                    com.digitar.mintx.utils.AdManager.showInterstitialAd(this) {
                        // After ad closes, smoothly show quiz content
                        isShowingInterstitial = false
                        transitionToContent()
                        updateUIForIndex(viewModel.currentIndex.value ?: 0)
                    }
                } else {
                    // Subsequent updates or already visible - just transition smoothly
                    transitionToContent()
                    updateUIForIndex(viewModel.currentIndex.value ?: 0)
                }
            } else {
                transitionToSkeleton()
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
            // Only handle SHOWING the skeleton here.
            // We delegate HIDING (transition to content) to the questions observer
            // to ensure accurate timing with Ad display and data binding.
            if (isLoading) {
                transitionToSkeleton()
            }
        }

        viewModel.currentScore.observe(this) { score ->
            binding.clQuizContent.header.tvCurrentScore.text = "Pts: $score"
        }

        viewModel.scoreUpdateEvent.observe(this) { delta ->
            val animView = binding.clQuizContent.header.tvScoreAnim
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

        if (index > 0) {
            // Animate transition for next questions - ONLY question and options
            val viewsToAnimate = listOf(binding.clQuizContent.question.root, binding.clQuizContent.options.root)
            
            viewsToAnimate.forEach { view ->
                view.animate()
                    .alpha(0f)
                    .translationX(-50f)
                    .setDuration(200)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        // We only need to bind once, so check if this is the last view
                        if (view == viewsToAnimate.last()) {
                            bindQuestionData(index, questions)
                        }
                        
                        view.translationX = 50f
                        view.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(200)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    }
                    .start()
            }
        } else {
            // No animation for first load (handled by initial fade in observeViewModel)
            bindQuestionData(index, questions)
        }
    }

    private fun bindQuestionData(index: Int, questions: List<QuizQuestion>) {
        val question = questions[index]
        
        startTimer()

        binding.clQuizContent.header.tvQuizProgressText.text = "Question ${index + 1} of ${questions.size}"

        binding.clQuizContent.header.pbQuizProgress.progress = ((index + 1) * 100) / questions.size
        
        binding.clQuizContent.question.tvQuestionText.text = "Q${index + 1}. ${question.question}"
        
        binding.layoutFloating.layoutHintOptions.visibility = View.GONE

        // Update Category Label
        val categoryName = question.category
        binding.clQuizContent.question.tvCategoryLabel.text = categoryName.uppercase()

        // Update Difficulty Chips
        val difficulty = question.difficulty.lowercase()
        binding.clQuizContent.question.chipEasy.visibility = if (difficulty == "easy") View.VISIBLE else View.GONE
        binding.clQuizContent.question.chipMedium.visibility = if (difficulty == "medium") View.VISIBLE else View.GONE
        binding.clQuizContent.question.chipHard.visibility = if (difficulty == "hard") View.VISIBLE else View.GONE

        displayOptions(question, index)
    }

    private fun displayOptions(question: QuizQuestion, questionIndex: Int) {
        val optionContainers = listOf(binding.clQuizContent.options.btnOption1, binding.clQuizContent.options.btnOption2, binding.clQuizContent.options.btnOption3, binding.clQuizContent.options.btnOption4)
        val optionTexts = listOf(binding.clQuizContent.options.tvOptionText1, binding.clQuizContent.options.tvOptionText2, binding.clQuizContent.options.tvOptionText3, binding.clQuizContent.options.tvOptionText4)
        val selectedAnswer = viewModel.userAnswers.value?.get(questionIndex)

        optionContainers.forEach { container ->
            // STOP any running animations from previous question
            val background = container.background
            if (background is android.graphics.drawable.AnimationDrawable) {
                background.stop()
            }
            
            // RESET to default state explicitly
            container.setBackgroundResource(R.drawable.pill_choice_selector)
            container.visibility = View.GONE
            container.isActivated = false
            container.isSelected = false
            container.isEnabled = true
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
            // Show correct answer normally (Green) without blinking gold
            showCorrectAnswer(question, isHintReveal = false)
        }
    }

    private fun showCorrectAnswer(question: QuizQuestion, isHintReveal: Boolean = false) {
        val optionContainers = listOf(binding.clQuizContent.options.btnOption1, binding.clQuizContent.options.btnOption2, binding.clQuizContent.options.btnOption3, binding.clQuizContent.options.btnOption4)
        val validAnswers = question.answers.filterValues { it != null }
        val answerKeys = validAnswers.keys.toList()

        val actualCorrectKey = question.correctAnswers.entries.find { it.value == "true" }?.key
        if (actualCorrectKey != null) {
            val baseKeyIdx = answerKeys.indexOf(actualCorrectKey.replace("_correct", ""))
            if (baseKeyIdx != -1 && baseKeyIdx < optionContainers.size) {
                val correctOption = optionContainers[baseKeyIdx]
                
                // Set activated state (Standard Green)
                correctOption.isActivated = true
                
                if (isHintReveal) {
                    // ONLY for Hints: Apply blinking gold border animation
                    correctOption.setBackgroundResource(R.drawable.blink_gold_border)
                    val animationDrawable = correctOption.background as? android.graphics.drawable.AnimationDrawable
                    animationDrawable?.start()
                    
                    // Stop animation after 5 seconds OR when view is recycled/hidden
                    correctOption.postDelayed({
                        if (correctOption.background is android.graphics.drawable.AnimationDrawable) {
                            animationDrawable?.stop()
                            // Restore normal activated background (Green)
                            correctOption.setBackgroundResource(R.drawable.pill_choice_selector)
                            correctOption.isActivated = true
                        }
                    }, 5000)
                } else {
                    // Standard reveal (Wrong answer selected): Just ensure it's green
                    correctOption.setBackgroundResource(R.drawable.pill_choice_selector)
                }
            }
        }
    }

    private fun disableOptions() {
        binding.clQuizContent.options.btnOption1.isEnabled = false
        binding.clQuizContent.options.btnOption2.isEnabled = false
        binding.clQuizContent.options.btnOption3.isEnabled = false
        binding.clQuizContent.options.btnOption4.isEnabled = false
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
                binding.clQuizContent.question.viewTimerBorder.setProgress(progress, color)
                
                // Update Analog Clock Timer (Floating)
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                
                binding.layoutFloating.viewFloatingTimer.setProgress(progress, color)
                binding.layoutFloating.viewFloatingTimer.setText(timeStr)
                
                // Stop Shimmer if it's running
                if (binding.layoutFloating.cardFloatingTimer.isShimmerStarted) {
                    binding.layoutFloating.cardFloatingTimer.stopShimmer()
                    binding.layoutFloating.cardFloatingTimer.setShimmer(null) // Remove shimmer fully
                }
                if (binding.layoutFloating.cardHint.isShimmerStarted) {
                    binding.layoutFloating.cardHint.stopShimmer()
                    binding.layoutFloating.cardHint.setShimmer(null) // Remove shimmer fully
                }
                
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
                binding.clQuizContent.question.viewTimerBorder.setProgress(0f, ContextCompat.getColor(this@QuizActivity, R.color.accent_red))
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
        
        // Show interstitial ad before summary
        isShowingInterstitial = true
        com.digitar.mintx.utils.AdManager.showInterstitialAd(this) {
            isShowingInterstitial = false
            // After ad closes, show summary
            binding.clSummary.root.visibility = View.VISIBLE
            binding.clQuizContent.root.visibility = View.GONE
            
            binding.clSummary.rowTotal.apply {
                tvLabel.text = "Total Questions"
                tvValue.text = summary.totalQuestions.toString()
            }
            binding.clSummary.rowCorrect.apply {
                tvLabel.text = "Correct Answers"
                tvValue.text = summary.correctCount.toString()
                tvValue.setTextColor(ContextCompat.getColor(this@QuizActivity, R.color.mint_green))
            }
            binding.clSummary.rowCorrectPoints.apply {
                tvLabel.text = "Points Earned"
                tvValue.text = "+${summary.correctPoints}"
                tvValue.setTextColor(ContextCompat.getColor(this@QuizActivity, R.color.mint_green))
            }
            binding.clSummary.rowWrong.apply {
                tvLabel.text = "Wrong Answers"
                tvValue.text = summary.wrongCount.toString()
                tvValue.setTextColor(ContextCompat.getColor(this@QuizActivity, R.color.accent_red))
            }
            binding.clSummary.rowNegativePoints.apply {
                tvLabel.text = "Points Deducted"
                tvValue.text = "-${summary.negativePoints}"
                tvValue.setTextColor(ContextCompat.getColor(this@QuizActivity, R.color.accent_red))
            }
            binding.clSummary.rowSkipped.apply {
                tvLabel.text = "Skipped"
                tvValue.text = summary.skippedCount.toString()
            }
            
            binding.clSummary.tvTotalPoints.text = "Total Points: ${summary.totalPoints}"

            val accuracy = if (summary.totalQuestions > 0) {
                ((summary.correctCount.toDouble() / summary.totalQuestions) * 100).toInt()
            } else 0
            
            binding.clSummary.tvSummaryAccuracy.text = "$accuracy%"
            
            val indicatorColor = when {
                accuracy >= 70 -> R.color.mint_green
                accuracy >= 40 -> R.color.mint_gold
                else -> R.color.accent_red
            }
            
            binding.clSummary.pbSummaryRing.setIndicatorColor(ContextCompat.getColor(this@QuizActivity, indicatorColor))
            binding.clSummary.pbSummaryRing.setProgressCompat(accuracy, true)
            
            // Update Level UI
            val currentXP = viewModel.totalXP.value ?: 0
            updateLevelUI(currentXP)
        }
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
        
        // Show blur overlay when app is minimized during an active quiz
        // BUT NOT when showing an ad or interstitial
        if (!isShowingAd && !isShowingInterstitial &&
            binding.clSummary.root.visibility != View.VISIBLE && 
            viewModel.questions.value?.isNotEmpty() == true) {
            showBlurOverlay()
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Strict Mode Check: If overlay is visible, it means user minimized the app.
        // BUT if ad or interstitial was showing, this is expected behavior - don't terminate
        if (binding.overlayBlurWarning.root.visibility == View.VISIBLE && !isShowingAd && !isShowingInterstitial) {
            // Update UI to show termination message
            binding.overlayBlurWarning.tvWarningTitle.text = "Quiz Terminated"
            binding.overlayBlurWarning.tvWarningMessage.text = "Minimizing the app is not allowed during the quiz."
            binding.overlayBlurWarning.btnResumeQuiz.visibility = View.GONE
            binding.overlayBlurWarning.ivWarningIcon.setColorFilter(ContextCompat.getColor(this, R.color.error))
            
            // Keep warning visible for 2 seconds before quitting
            binding.root.postDelayed({
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }, 2000)
            return
        }
        
        // Hide blur overlay when app returns (failsafe)
        hideBlurOverlay()
        
        // Restart timer if quiz is active AND not showing hint
        if (!isShowingHint && 
            binding.clSummary.root.visibility != View.VISIBLE && 
            viewModel.questions.value?.isNotEmpty() == true) {
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

    private fun updateLevelUI(totalPoints: Long) {
        val levelInfo = LevelUtils.calculateLevelInfo(totalPoints)
        
        binding.clSummary.tvLevelCurrent.text = "Level ${levelInfo.level}"
        
        val pointsInLevel = levelInfo.currentPoints - levelInfo.minPoints
        val totalPointsForLevel = levelInfo.maxPoints - levelInfo.minPoints
        
        binding.clSummary.tvLevelPoints.text = "$pointsInLevel / $totalPointsForLevel XP"
        binding.clSummary.pbLevelProgress.progress = levelInfo.progressPercent
        
        binding.clSummary.tvLevelNextInfo.text = "${levelInfo.pointsToNextLevel} XP to Level ${levelInfo.level + 1}"
    }

    private fun showBlurOverlay() {
        // Reset to Pause state
        binding.overlayBlurWarning.tvWarningTitle.text = "Quiz Paused"
        binding.overlayBlurWarning.tvWarningMessage.text = "Please don't minimize the app during the quiz. Your timer is still running!"
        binding.overlayBlurWarning.ivWarningIcon.setColorFilter(ContextCompat.getColor(this, R.color.warning))
        binding.overlayBlurWarning.btnResumeQuiz.text = "Resume Quiz"
        binding.overlayBlurWarning.btnResumeQuiz.visibility = View.VISIBLE
        binding.overlayBlurWarning.btnQuitQuiz.visibility = View.GONE
        
        // Reset Resume button listener to simple hide
        binding.overlayBlurWarning.btnResumeQuiz.setOnClickListener { 
            hideBlurOverlay() 
        }
        
        binding.overlayBlurWarning.root.visibility = View.VISIBLE
    }

    private fun hideBlurOverlay() {
        binding.overlayBlurWarning.root.visibility = View.GONE
    }

    private fun setupBottomNavigation() {
        // Set 'Quiz' as active by default since we are in Quiz Screen
        binding.bottomNavigation.bottomNavigation.selectedItemId = R.id.navigation_quiz

        // Setup resume button click (Default)
        binding.overlayBlurWarning.btnResumeQuiz.setOnClickListener {
            hideBlurOverlay()
        }
        
        binding.bottomNavigation.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    showLeaveQuizConfirmation {
                        val intent = android.content.Intent(this, MainActivity::class.java)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                        intent.putExtra("NAV_ID", R.id.navigation_home)
                        startActivity(intent)
                        finish()
                    }
                    false
                }
                R.id.navigation_quiz -> {
                    true
                }
                R.id.navigation_earn, R.id.navigation_wallet, R.id.navigation_prediction -> {
                    showLeaveQuizConfirmation {
                        val intent = android.content.Intent(this, MainActivity::class.java)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                        intent.putExtra("NAV_ID", item.itemId)
                        startActivity(intent)
                        finish()
                    }
                    false
                }
                else -> false
            }
        }
    }

    private fun showLeaveQuizConfirmation(onConfirm: () -> Unit) {
        if (binding.clSummary.root.visibility == View.VISIBLE) {
            onConfirm()
            return
        }

        // Use Blur Overlay for Confirmation
        binding.overlayBlurWarning.tvWarningTitle.text = "Leave Quiz?"
        binding.overlayBlurWarning.tvWarningMessage.text = "Are you sure you want to leave? Your progress will be lost."
        binding.overlayBlurWarning.ivWarningIcon.setColorFilter(ContextCompat.getColor(this, R.color.mint_gold))
        
        binding.overlayBlurWarning.btnResumeQuiz.text = "Stay"
        binding.overlayBlurWarning.btnResumeQuiz.visibility = View.VISIBLE
        binding.overlayBlurWarning.btnQuitQuiz.visibility = View.VISIBLE
        
        // Setup Button Actions
        binding.overlayBlurWarning.btnQuitQuiz.setOnClickListener {
            hideBlurOverlay()
            onConfirm()
        }
        
        binding.overlayBlurWarning.btnResumeQuiz.setOnClickListener {
            hideBlurOverlay()
        }
        
        binding.overlayBlurWarning.root.visibility = View.VISIBLE
    }

    private fun closeHintOptions() {
        if (binding.layoutFloating.layoutHintOptions.visibility == View.VISIBLE) {
            binding.layoutFloating.layoutHintOptions.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    binding.layoutFloating.layoutHintOptions.visibility = View.GONE
                }
                .start()
        }
    }

    private fun toggleHintOptions() {
        // Prevent multiple rapid clicks
        if (isTogglingHint) return
        isTogglingHint = true
        
        val isVisible = binding.layoutFloating.layoutHintOptions.visibility == View.VISIBLE
        
        if (isVisible) {
            // Close with animation
            binding.layoutFloating.layoutHintOptions.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    binding.layoutFloating.layoutHintOptions.visibility = View.GONE
                    isTogglingHint = false
                }
                .start()
        } else {
            // Position and open with animation
            positionHintOptionsBasedOnLocation()
            
            binding.layoutFloating.layoutHintOptions.alpha = 0f
            binding.layoutFloating.layoutHintOptions.scaleX = 0.8f
            binding.layoutFloating.layoutHintOptions.scaleY = 0.8f
            binding.layoutFloating.layoutHintOptions.visibility = View.VISIBLE
            
            binding.layoutFloating.layoutHintOptions.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    isTogglingHint = false
                }
                .start()
        }
    }

    private fun positionHintOptionsBasedOnLocation() {
        // Use post to ensure parent layout is complete
        binding.layoutFloating.cardHint.post {
            // Measure the view first since it might be GONE
            binding.layoutFloating.layoutHintOptions.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            
            val optionsWidth = binding.layoutFloating.layoutHintOptions.measuredWidth
            val optionsHeight = binding.layoutFloating.layoutHintOptions.measuredHeight
            
            val parent = binding.layoutFloating.cardHint.parent as View
            val screenWidth = parent.width
            
            // Ensure parent has valid dimensions
            if (screenWidth == 0) {
                // Fallback positioning if parent not ready
                binding.layoutFloating.layoutHintOptions.x = 16f
                binding.layoutFloating.layoutHintOptions.y = binding.layoutFloating.cardHint.y
                return@post
            }
            
            val hintCenterX = binding.layoutFloating.cardHint.x + binding.layoutFloating.cardHint.width / 2
            
            // Determine if hint is on the left or right side of screen
            val isOnLeftSide = hintCenterX < screenWidth / 2
            
            // Position hint options vertically aligned with card_hint
            // Since both are 48dp usually, this centers them vertically relative to each other
            val hintY = binding.layoutFloating.cardHint.y
            binding.layoutFloating.layoutHintOptions.y = hintY + (binding.layoutFloating.cardHint.height - optionsHeight) / 2
            
            if (isOnLeftSide) {
                // Hint is on left side, show options to the right
                binding.layoutFloating.layoutHintOptions.x = binding.layoutFloating.cardHint.x + binding.layoutFloating.cardHint.width + 12
            } else {
                // Hint is on right side, show options to the left
                binding.layoutFloating.layoutHintOptions.x = binding.layoutFloating.cardHint.x - optionsWidth - 12
            }
        }
    }



    private fun transitionToContent() {
        if (binding.clQuizContent.root.visibility == View.VISIBLE && binding.clQuizContent.root.alpha == 1f) return
        
        binding.layoutSkeleton.root.animate()
            .alpha(0f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.layoutSkeleton.root.visibility = View.GONE
            }
            .start()
            
        binding.clQuizContent.root.visibility = View.VISIBLE
        binding.clQuizContent.root.alpha = 0f
        binding.clQuizContent.root.translationY = 50f
        binding.clQuizContent.root.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun transitionToSkeleton() {
        if (binding.layoutSkeleton.root.visibility == View.VISIBLE && binding.layoutSkeleton.root.alpha == 1f) return

        binding.clQuizContent.root.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.clQuizContent.root.visibility = View.GONE
            }
            .start()

        binding.layoutSkeleton.root.visibility = View.VISIBLE
        binding.layoutSkeleton.root.alpha = 0f
        binding.layoutSkeleton.root.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }
}
