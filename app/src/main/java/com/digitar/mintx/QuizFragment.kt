package com.digitar.mintx

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import java.util.ArrayList
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.digitar.mintx.data.model.QuizQuestion
import com.digitar.mintx.data.repository.QuizRepository
import com.digitar.mintx.databinding.FragmentQuizBinding
import com.digitar.mintx.ui.quiz.QuizCategoryBottomSheet
import com.digitar.mintx.ui.quiz.QuizViewModel
import java.util.Locale

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuizViewModel(QuizRepository(requireContext())) as T
            }
        }
    }

    private var countDownTimer: CountDownTimer? = null
    private val TIMER_DURATION = 120000L // 2 minutes

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeViewModel()
        
        // Listen for category selection from Home or elsewhere
        parentFragmentManager.setFragmentResultListener("quiz_request", viewLifecycleOwner) { _, bundle ->
            val categories = bundle.getStringArrayList("categories")
            if (!categories.isNullOrEmpty()) {
                startQuizWithCategories(categories)
            }
        }

        // Automatically show category selector if quiz hasn't started and no questions loaded
        if (viewModel.questions.value.isNullOrEmpty()) {
            showQuizCategorySelector()
        }
    }

    private fun showQuizCategorySelector() {
        val bottomSheet = QuizCategoryBottomSheet.newInstance()
        bottomSheet.onQuizStarted = { categories ->
            startQuizWithCategories(categories)
        }
        bottomSheet.show(childFragmentManager, QuizCategoryBottomSheet.TAG)
    }

    private fun startQuizWithCategories(categories: List<String>) {
        // Update header to show category names
        val categoryText = if (categories.size > 2) {
            "${categories[0]} • ${categories[1]} +${categories.size - 2}"
        } else {
            categories.joinToString(" • ")
        }
        binding.tvQuizRewardLabel.text = categoryText
        viewModel.fetchQuestions(categories = categories)
    }

    private fun setupClickListeners() {
        binding.btnNext.setOnClickListener {
            viewModel.nextQuestion()
        }

        binding.btnPrev.setOnClickListener {
            viewModel.prevQuestion()
        }

        binding.btnFinish.setOnClickListener {
            // Navigate back or perform finish action
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun observeViewModel() {
        viewModel.questions.observe(viewLifecycleOwner) { questions ->
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

        viewModel.currentIndex.observe(viewLifecycleOwner) { index ->
            updateUIForIndex(index)
        }

        viewModel.quizFinished.observe(viewLifecycleOwner) { isFinished ->
            if (isFinished) {
                showSummary()
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // If loading but we already have questions, just show progress bar
            // If loading and no questions, the skeleton is already visible
            binding.pbQuizProgress.isIndeterminate = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUIForIndex(index: Int) {
        val questions = viewModel.questions.value ?: return
        if (index >= questions.size) return

        val question = questions[index]
        
        // Reset and start timer
        startTimer()

        // Update Progress
        binding.tvQuizProgress.text = "Question ${index + 1} of ${questions.size}"
        binding.pbQuizProgress.progress = ((index + 1) * 100) / questions.size
        
        // Question Text
        binding.tvQuestionText.text = question.question

        // Display Options
        displayOptions(question, index)

        // Update Nav Buttons
        binding.btnPrev.isEnabled = index > 0
        binding.btnPrev.alpha = if (index > 0) 1.0f else 0.5f
        binding.btnNext.text = if (index == questions.size - 1) "Finish" else "Next"
    }

    private fun displayOptions(question: QuizQuestion, questionIndex: Int) {
        val options = listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)
        val selectedAnswer = viewModel.userAnswers.value?.get(questionIndex)

        // Reset states
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
            view.text = answerText

            // Restore selection if exists
            if (selectedAnswer == key) {
                highlightSelection(view, key, question)
                // If answered, disable buttons for this question
                options.forEach { it.isEnabled = false }
            }

            view.setOnClickListener {
                stopTimer()
                viewModel.selectAnswer(questionIndex, key)
                highlightSelection(view, key, question)
                options.forEach { it.isEnabled = false }
                
                // Optional: Auto move to next after delay
                binding.root.postDelayed({
                    if (isAdded && viewModel.currentIndex.value == questionIndex) {
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
            // Also show the correct one
            showCorrectAnswer(question)
        }
    }

    private fun showCorrectAnswer(question: QuizQuestion) {
        val options = listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)
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
                val progress = ((millisUntilFinished.toFloat() / TIMER_DURATION.toFloat()) * 100).toInt()
                binding.pbTimerRing.progress = progress

                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.tvTimerText.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                
                // Visual warning color
                if (millisUntilFinished < 15000) {
                    binding.tvTimerText.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_red))
                    binding.pbTimerRing.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.accent_red))
                } else {
                    binding.tvTimerText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    binding.pbTimerRing.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.mint_gold))
                }
            }

            override fun onFinish() {
                binding.tvTimerText.text = "00:00"
                Toast.makeText(requireContext(), "Time's up!", Toast.LENGTH_SHORT).show()
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
        
        // Fill Summary Data
        binding.rowTotal.apply {
            tvLabel.text = "Total Questions"
            tvValue.text = summary.totalQuestions.toString()
        }
        binding.rowCorrect.apply {
            tvLabel.text = "Correct Answers"
            tvValue.text = summary.correctCount.toString()
            tvValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.mint_green))
        }
        binding.rowWrong.apply {
            tvLabel.text = "Wrong Answers"
            tvValue.text = summary.wrongCount.toString()
            tvValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_red))
        }
        binding.rowSkipped.apply {
            tvLabel.text = "Skipped"
            tvValue.text = summary.skippedCount.toString()
        }
        
        binding.tvTotalPoints.text = "Total Points: ${summary.totalPoints}"

        // Summary Ring Logic
        val accuracy = if (summary.totalQuestions > 0) {
            ((summary.correctCount.toDouble() / summary.totalQuestions) * 100).toInt()
        } else 0
        
        binding.tvSummaryAccuracy.text = "$accuracy%"
        
        // Determine Color based on highest count
        val indicatorColor = when {
            summary.correctCount >= summary.wrongCount && summary.correctCount >= summary.skippedCount -> R.color.mint_green
            summary.wrongCount > summary.correctCount && summary.wrongCount >= summary.skippedCount -> R.color.accent_red
            else -> R.color.mint_gold
        }
        
        binding.pbSummaryRing.setIndicatorColor(ContextCompat.getColor(requireContext(), indicatorColor))
        binding.pbSummaryRing.setProgressCompat(accuracy, true)
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
    }

    override fun onResume() {
        super.onResume()
        // Resume timer or handle accordingly if a quiz is in progress
        if (binding.clSummary.visibility != View.VISIBLE && viewModel.questions.value?.isNotEmpty() == true) {
            startTimer()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimer()
        _binding = null
    }

    companion object {
        fun newInstance() = QuizFragment()
    }
}