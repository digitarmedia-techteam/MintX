package com.digitar.mintx

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.digitar.mintx.data.model.QuizQuestion
import com.digitar.mintx.data.repository.QuizRepository
import com.digitar.mintx.databinding.ActivityQuizBinding
import com.digitar.mintx.ui.quiz.QuizCategoryBottomSheet
import com.digitar.mintx.ui.quiz.QuizViewModel
import com.digitar.mintx.utils.AdManager.showRewardedAd
import com.digitar.mintx.utils.LevelUtils
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
    private var isShowingAd = false
    private var isShowingHint = false
    private var isShowingInterstitial = false
    private var isTogglingHint = false
    private var hintTapCount = 0
    private var hasShownHintInstruction = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Optimized Insets Handling for QuizActivity
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Pad the bottom navigation to act as margin from the bottom gesture bar
            binding.bottomNavigation.root.setPadding(0, 0, 0, systemBars.bottom)
            
            insets
        }

        setupClickListeners()
        setupBottomNavigation()
        observeViewModel()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.clSummary.root.visibility == View.VISIBLE) {
                    finishQuiz()
                } else {
                    showLeaveQuizConfirmation({ finishQuiz() }, {}) 
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
        
        binding.root.postDelayed({
            com.digitar.mintx.utils.AdManager.loadRewardedAd(this)
            com.digitar.mintx.utils.AdManager.loadInterstitialAd(this)
        }, 1000)
    }

    private fun finishQuiz() {
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
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
            showLeaveQuizConfirmation({ finishQuiz() }, {})
        }

        binding.clSummary.btnFinish.setOnClickListener {
            finishQuiz()
        }

        binding.clSummary.btnPlayAgain.setOnClickListener {
            binding.clSummary.root.visibility = View.GONE
            viewModel.restartQuiz()
        }
        
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
                    if (kotlin.math.abs(newX - initialX) > 10 || kotlin.math.abs(newY - initialY) > 10) {
                        hasMoved = true
                        view.animate().x(newX).y(newY).setDuration(0).start()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved) {
                        view.performClick()
                        hintTapCount++
                        val scale = 1f - (hintTapCount * 0.05f) 
                        view.animate().scaleX(scale).scaleY(scale).setDuration(80).withEndAction {
                                view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                            }.start()
                        
                        when (hintTapCount) {
                            1 -> {
                                hintTapCount = 0
                                toggleHintOptions()
                                view.animate().rotationBy(360f).setDuration(300).start()
                            }
                        }
                        view.removeCallbacks(null)
                        view.postDelayed({
                            if (hintTapCount < 3) {
                                hintTapCount = 0
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
            isShowingAd = true
            showRewardedAd(this,
                onRewardEarned = {
                    isShowingAd = false
                    binding.root.postDelayed({
                        if (!isFinishing) handleHintUsed()
                    }, 300)
                },
                onAdClosed = { isShowingAd = false }
            )
        }
    }

    private fun handleHintUsed() {
        val index = viewModel.currentIndex.value ?: 0
        val questions = viewModel.questions.value ?: return
        
        if (index < questions.size) {
            isShowingHint = true
            stopTimer()
            disableOptions()
            showCorrectAnswer(questions[index], isHintReveal = true)
            Toast.makeText(this, "Answer Revealed!", Toast.LENGTH_SHORT).show()
            
            binding.root.postDelayed({
                if (!isFinishing && viewModel.currentIndex.value == index) {
                    isShowingHint = false
                    viewModel.nextQuestion()
                }
            }, 3000)
        }
    }

    private fun observeViewModel() {
        viewModel.questions.observe(this) { questions ->
            if (questions.isNotEmpty()) {
                if (binding.layoutSkeleton.root.visibility == View.VISIBLE && binding.layoutSkeleton.root.alpha > 0f) {
                    isShowingInterstitial = true
                    com.digitar.mintx.utils.AdManager.showInterstitialAd(this) {
                        isShowingInterstitial = false
                        transitionToContent()
                        updateUIForIndex(viewModel.currentIndex.value ?: 0)
                    }
                } else {
                    transitionToContent()
                    updateUIForIndex(viewModel.currentIndex.value ?: 0)
                }
            } else {
                transitionToSkeleton()
            }
        }

        viewModel.currentIndex.observe(this) { index -> updateUIForIndex(index) }

        viewModel.quizFinished.observe(this) { isFinished ->
            if (isFinished) showSummary()
        }

        viewModel.loading.observe(this) { isLoading ->
            if (isLoading) transitionToSkeleton()
        }

        viewModel.currentScore.observe(this) { score ->
            binding.clQuizContent.header.tvCurrentScore.text = "Pts: $score"
        }

        viewModel.scoreUpdateEvent.observe(this) { delta ->
            val animView = binding.clQuizContent.header.tvScoreAnim
            animView.text = if (delta > 0) "+$delta" else "$delta"
            animView.setTextColor(ContextCompat.getColor(this, if (delta > 0) R.color.mint_green else R.color.accent_red))
            animView.visibility = View.VISIBLE
            animView.alpha = 0f
            animView.translationY = 20f

            animView.animate().alpha(1f).translationY(-20f).setDuration(400).setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    animView.animate().alpha(0f).setDuration(300).setStartDelay(200)
                        .withEndAction { animView.visibility = View.GONE }.start()
                }.start()
        }

        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUIForIndex(index: Int) {
        val questions = viewModel.questions.value ?: return
        if (index >= questions.size) return

        if (index > 0) {
            val viewsToAnimate = listOf(binding.clQuizContent.question.root, binding.clQuizContent.options.root)
            viewsToAnimate.forEach { view ->
                view.animate().alpha(0f).translationX(-50f).setDuration(200).setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        if (view == viewsToAnimate.last()) bindQuestionData(index, questions)
                        view.translationX = 50f
                        view.animate().alpha(1f).translationX(0f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
                    }.start()
            }
        } else {
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

        binding.clQuizContent.question.tvCategoryLabel.text = question.category.uppercase()
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
            val background = container.background
            if (background is android.graphics.drawable.AnimationDrawable) background.stop()
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
            
            // Update Daily Stats for Contribution Graph
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val todayKey = com.digitar.mintx.utils.StreakUtils.getTodayDateKey()
                // Update map field: "dailyStats.yyyy-MM-dd"
                db.collection("users").document(uid)
                    .update("dailyStats.$todayKey", com.google.firebase.firestore.FieldValue.increment(1))
                    .addOnFailureListener {
                         // Ignore
                    }
            }
        } else {
            view.isSelected = true
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
                correctOption.isActivated = true
                if (isHintReveal) {
                    correctOption.setBackgroundResource(R.drawable.blink_gold_border)
                    (correctOption.background as? android.graphics.drawable.AnimationDrawable)?.start()
                    correctOption.postDelayed({
                         if (correctOption.background is android.graphics.drawable.AnimationDrawable) {
                             (correctOption.background as? android.graphics.drawable.AnimationDrawable)?.stop()
                             correctOption.setBackgroundResource(R.drawable.pill_choice_selector)
                             correctOption.isActivated = true
                         }
                    }, 5000)
                } else {
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
                val progress = (millisUntilFinished.toFloat() / TIMER_DURATION.toFloat()) * 100f
                val colorRes = when {
                    progress > 60f -> R.color.mint_green
                    progress > 30f -> R.color.yellow
                    else -> R.color.accent_red
                }
                val color = ContextCompat.getColor(this@QuizActivity, colorRes)
                binding.clQuizContent.question.viewTimerBorder.setProgress(progress, color)
                
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.layoutFloating.viewFloatingTimer.setProgress(progress, color)
                binding.layoutFloating.viewFloatingTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds))
                
                // Shimmer check removed from here
                
                val scale = 1f + (0.05f * kotlin.math.sin(System.currentTimeMillis() / 200.0).toFloat())
                binding.layoutFloating.cardFloatingTimer.scaleX = scale
                binding.layoutFloating.cardFloatingTimer.scaleY = scale

                if (millisUntilFinished <= 40000) {
                    ObjectAnimator.ofFloat(binding.layoutFloating.cardHint, "translationX", 0f, 10f, -10f, 10f, -10f, 0f).setDuration(500).start()
                }
            }
            override fun onFinish() {
                binding.clQuizContent.question.viewTimerBorder.setProgress(0f, ContextCompat.getColor(this@QuizActivity, R.color.accent_red))
                Toast.makeText(this@QuizActivity, "Time's up!", Toast.LENGTH_SHORT).show()
                viewModel.nextQuestion()
            }
        }.start()
    }

    private fun stopTimer() { countDownTimer?.cancel() }

    private fun showSummary() {
        stopTimer()
        viewModel.saveQuizResults()
        val summary = viewModel.getQuizSummary()
        isShowingInterstitial = true
        com.digitar.mintx.utils.AdManager.showInterstitialAd(this) {
            isShowingInterstitial = false
            binding.clSummary.root.visibility = View.VISIBLE
            binding.clQuizContent.root.visibility = View.GONE
            
            binding.clSummary.rowTotal.apply { tvLabel.text = "Total Questions"; tvValue.text = summary.totalQuestions.toString() }
            binding.clSummary.rowCorrect.apply { tvLabel.text = "Correct Answers"; tvValue.text = summary.correctCount.toString(); tvValue.setTextColor(ContextCompat.getColor(this@QuizActivity, R.color.mint_green)) }
            binding.clSummary.rowCorrectPoints.apply { tvLabel.text = "Points Earned"; tvValue.text = "+${summary.correctPoints}"; tvValue.setTextColor(ContextCompat.getColor(this@QuizActivity, R.color.mint_green)) }
            binding.clSummary.rowWrong.apply { tvLabel.text = "Wrong Answers"; tvValue.text = summary.wrongCount.toString(); tvValue.setTextColor(ContextCompat.getColor(this@QuizActivity, R.color.accent_red)) }
            binding.clSummary.rowNegativePoints.apply { tvLabel.text = "Points Deducted"; tvValue.text = "-${summary.negativePoints}"; tvValue.setTextColor(ContextCompat.getColor(this@QuizActivity, R.color.accent_red)) }
            binding.clSummary.rowSkipped.apply { tvLabel.text = "Skipped"; tvValue.text = summary.skippedCount.toString() }
            
            binding.clSummary.tvTotalPoints.text = "Total Points: ${summary.totalPoints}"
            val accuracy = if (summary.totalQuestions > 0) ((summary.correctCount.toDouble() / summary.totalQuestions) * 100).toInt() else 0
            binding.clSummary.tvSummaryAccuracy.text = "$accuracy%"
            binding.clSummary.pbSummaryRing.setProgressCompat(accuracy, true)
            updateLevelUI(viewModel.totalXP.value ?: 0)
        }
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
        if (!isShowingAd && !isShowingInterstitial && binding.clSummary.root.visibility != View.VISIBLE && viewModel.questions.value?.isNotEmpty() == true) {
            showBlurOverlay()
        }
    }

    override fun onResume() {
        super.onResume()
        if (binding.overlayBlurWarning.root.visibility == View.VISIBLE && !isShowingAd && !isShowingInterstitial) {
            binding.overlayBlurWarning.tvWarningTitle.text = "Quiz Terminated"
            binding.overlayBlurWarning.tvWarningMessage.text = "Minimizing the app is not allowed during the quiz."
            binding.overlayBlurWarning.btnResumeQuiz.visibility = View.GONE
            binding.overlayBlurWarning.ivWarningIcon.setColorFilter(ContextCompat.getColor(this, R.color.error))
            binding.root.postDelayed({ finishQuiz() }, 2000)
            return
        }
        hideBlurOverlay()
        if (!isShowingHint && binding.clSummary.root.visibility != View.VISIBLE && viewModel.questions.value?.isNotEmpty() == true) {
            startTimer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
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
        binding.overlayBlurWarning.tvWarningTitle.text = "Quiz Paused"
        binding.overlayBlurWarning.tvWarningMessage.text = "Please don't minimize the app."
        binding.overlayBlurWarning.btnResumeQuiz.text = "Resume Quiz"
        binding.overlayBlurWarning.btnResumeQuiz.visibility = View.VISIBLE
        binding.overlayBlurWarning.btnQuitQuiz.visibility = View.GONE
        binding.overlayBlurWarning.btnResumeQuiz.setOnClickListener { hideBlurOverlay() }
        binding.overlayBlurWarning.root.visibility = View.VISIBLE
    }

    private fun hideBlurOverlay() {
        binding.overlayBlurWarning.root.visibility = View.GONE
    }

    private fun setupBottomNavigation() {
        val bottomBar = binding.bottomNavigation.root as com.digitar.mintx.ui.components.SmoothBottomBar
        val quizIndex = bottomBar.getIndexForMenuId(R.id.navigation_quiz)
        bottomBar.selectItem(quizIndex, animate = false)

        bottomBar.setOnItemSelectedListener { index ->
            val itemId = bottomBar.getMenuIdForIndex(index)
            if (itemId == R.id.navigation_quiz) return@setOnItemSelectedListener // Already here

            if (R.id.navigation_home == itemId || R.id.navigation_earn == itemId || R.id.navigation_wallet == itemId || R.id.navigation_prediction == itemId) {
                showLeaveQuizConfirmation(
                    onConfirm = {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        intent.putExtra("NAV_ID", itemId)
                        startActivity(intent)
                        finishQuiz()
                    },
                    onStay = {
                        // Revert selection back to Quiz if they stay
                        bottomBar.selectItem(quizIndex, animate = true)
                    }
                )
            }
        }
    }

    private fun showLeaveQuizConfirmation(onConfirm: () -> Unit, onStay: () -> Unit) {
        if (binding.clSummary.root.visibility == View.VISIBLE) {
            onConfirm()
            return
        }
        binding.overlayBlurWarning.tvWarningTitle.text = "Leave Quiz?"
        binding.overlayBlurWarning.tvWarningMessage.text = "Are you sure you want to leave? Progress will be lost."
        binding.overlayBlurWarning.btnResumeQuiz.text = "Stay"
        binding.overlayBlurWarning.btnResumeQuiz.visibility = View.VISIBLE
        binding.overlayBlurWarning.btnQuitQuiz.visibility = View.VISIBLE
        
        binding.overlayBlurWarning.btnQuitQuiz.setOnClickListener {
            hideBlurOverlay()
            onConfirm()
        }
        binding.overlayBlurWarning.btnResumeQuiz.setOnClickListener { 
            hideBlurOverlay()
            onStay()
        }
        binding.overlayBlurWarning.root.visibility = View.VISIBLE
    }

    private fun positionHintOptionsBasedOnLocation() {
        binding.layoutFloating.cardHint.post {
            binding.layoutFloating.layoutHintOptions.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val optionsWidth = binding.layoutFloating.layoutHintOptions.measuredWidth
            val optionsHeight = binding.layoutFloating.layoutHintOptions.measuredHeight
            val parent = binding.layoutFloating.cardHint.parent as View
            if (parent.width == 0) return@post
            
            val hintCenterX = binding.layoutFloating.cardHint.x + binding.layoutFloating.cardHint.width / 2
            binding.layoutFloating.layoutHintOptions.y = binding.layoutFloating.cardHint.y + (binding.layoutFloating.cardHint.height - optionsHeight) / 2
            
            if (hintCenterX < parent.width / 2) {
                binding.layoutFloating.layoutHintOptions.x = binding.layoutFloating.cardHint.x + binding.layoutFloating.cardHint.width + 12
            } else {
                binding.layoutFloating.layoutHintOptions.x = binding.layoutFloating.cardHint.x - optionsWidth - 12
            }
        }
    }

    private fun toggleHintOptions() {
        if (isTogglingHint) return
        isTogglingHint = true
        if (binding.layoutFloating.layoutHintOptions.visibility == View.VISIBLE) {
            binding.layoutFloating.layoutHintOptions.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(200).withEndAction {
                binding.layoutFloating.layoutHintOptions.visibility = View.GONE
                isTogglingHint = false
            }.start()
        } else {
            positionHintOptionsBasedOnLocation()
            binding.layoutFloating.layoutHintOptions.alpha = 0f
            binding.layoutFloating.layoutHintOptions.scaleX = 0.8f
            binding.layoutFloating.layoutHintOptions.scaleY = 0.8f
            binding.layoutFloating.layoutHintOptions.visibility = View.VISIBLE
            binding.layoutFloating.layoutHintOptions.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(250).withEndAction {
                isTogglingHint = false
            }.start()
        }
    }

    private fun closeHintOptions() {
        binding.layoutFloating.layoutHintOptions.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(200).withEndAction {
             binding.layoutFloating.layoutHintOptions.visibility = View.GONE
        }.start()
    }

    private fun transitionToContent() {
        if (binding.clQuizContent.root.visibility == View.VISIBLE && binding.clQuizContent.root.alpha == 1f) return
        binding.layoutSkeleton.root.animate().alpha(0f).setDuration(500).withEndAction { binding.layoutSkeleton.root.visibility = View.GONE }.start()
        binding.clQuizContent.root.visibility = View.VISIBLE
        binding.clQuizContent.root.alpha = 0f
        binding.clQuizContent.root.translationY = 50f
        binding.clQuizContent.root.animate().alpha(1f).translationY(0f).setDuration(500).start()
    }

    private fun transitionToSkeleton() {
        if (binding.layoutSkeleton.root.visibility == View.VISIBLE && binding.layoutSkeleton.root.alpha == 1f) return
        binding.clQuizContent.root.animate().alpha(0f).setDuration(300).withEndAction { binding.clQuizContent.root.visibility = View.GONE }.start()
        binding.layoutSkeleton.root.visibility = View.VISIBLE
        binding.layoutSkeleton.root.alpha = 0f
        binding.layoutSkeleton.root.animate().alpha(1f).setDuration(300).start()
    }
}
