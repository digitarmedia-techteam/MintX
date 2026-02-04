package com.digitar.mintx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.digitar.mintx.databinding.FragmentHomeBinding
import com.digitar.mintx.ui.quiz.QuizCategoryBottomSheet
import android.content.Intent
import java.util.ArrayList
import androidx.core.content.ContextCompat
import com.digitar.mintx.utils.StreakUtils
import com.digitar.mintx.RewardsStoreActivity
import com.digitar.mintx.ProfileActivity
import com.digitar.mintx.QuizActivity
import com.digitar.mintx.R

class HomeFragment : Fragment() {
    private var balanceAnimator: android.animation.ValueAnimator? = null
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var isStreakLoading = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.cardWallet.setOnClickListener {
            // Navigate to Wallet Details
        }
        
        binding.cardFeatured.setOnClickListener {
            // Navigate to Featured Event (Live Match/Quiz)
        }
        
        binding.btnQuickQuiz.setOnClickListener {
            navigateToQuiz()
        }

        binding.btnRedeemHome.setOnClickListener {
            startActivity(Intent(requireContext(), RewardsStoreActivity::class.java))
        }

        binding.reward.setOnClickListener {
            startActivity(Intent(requireContext(), RewardsStoreActivity::class.java))
        }

        // Setup Profile Click
        binding.navHeaderAvatar.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }
        
        // Show basic user data
        val sessionManager = com.digitar.mintx.utils.SessionManager(requireContext())
        val name = sessionManager.getUserName()
        if (name != null) {
            binding.tvGreeting.text = "Hello, $name"
            animateGreetingEmoji()
        }

        // Avatar Logic
        val photoUrl = sessionManager.getUserPhoto()
        if (!photoUrl.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.user_gif)
                .error(R.drawable.user_gif)
                .into(binding.navHeaderAvatar)
        }
        // Load Live GIF
        try {
            com.bumptech.glide.Glide.with(this)
                .load(R.drawable.live)
                .into(binding.ivLiveBadge)
        } catch (e: Exception) {
            // Fallback
        }
        setupFeaturedCard("")
        
        // Start streak dots loading animation
        animateStreakDotsLoader()
    }

    private fun setupFeaturedCard(countryCode: String) {
        val context = requireContext()
        val primaryColor: Int
        val darkColor: Int
        val buttonStartColor: Int
        val buttonEndColor: Int

        when (countryCode.uppercase()) {
            "IND" -> {
                primaryColor = android.graphics.Color.parseColor("#1565C0")
                darkColor = android.graphics.Color.parseColor("#0D47A1")
                buttonStartColor = android.graphics.Color.parseColor("#42A5F5") // Lighter Blue top
                buttonEndColor = android.graphics.Color.parseColor("#1565C0")  // Darker Blue bottom
            }
            "AUS" -> {
                primaryColor = android.graphics.Color.parseColor("#FF6F00")
                darkColor = android.graphics.Color.parseColor("#E65100")
                buttonStartColor = android.graphics.Color.parseColor("#FFCA28")
                buttonEndColor = android.graphics.Color.parseColor("#FF6F00")
            }
            else -> {
                primaryColor = android.graphics.Color.parseColor("#2E7D32")
                darkColor = android.graphics.Color.parseColor("#1B5E20")
                buttonStartColor = android.graphics.Color.parseColor("#66BB6A")
                buttonEndColor = android.graphics.Color.parseColor("#2E7D32")
            }
        }

        try {
            // 1. Update Card Overlay (Gradient) for readability
            val overlayBg = binding.viewGradientOverlay.background.mutate() as? android.graphics.drawable.GradientDrawable
            overlayBg?.let {
                // Smooth gradient from solid primary to transparent
                val start = androidx.core.graphics.ColorUtils.setAlphaComponent(primaryColor, 242) // 95%
                val center = androidx.core.graphics.ColorUtils.setAlphaComponent(primaryColor, 160) // 60%
                val end = androidx.core.graphics.ColorUtils.setAlphaComponent(primaryColor, 0) // Transparent
                
                it.colors = intArrayOf(start, center, end)
                it.orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
            }

            // 2. Update 3D Depth View (Bezel)
            val depthBg = binding.viewCardDepth.background.mutate() as? android.graphics.drawable.GradientDrawable
            depthBg?.setColor(darkColor)

            // 3. Update Predict Button (3D Effect)
            val btnBg = binding.btnPredictWin.background.mutate() as? android.graphics.drawable.LayerDrawable
            btnBg?.let { layer ->
                // Bottom Layer (Shadow/Depth) - Index 0
                val shadow = layer.getDrawable(0) as? android.graphics.drawable.GradientDrawable
                shadow?.setColor(darkColor)

                // Top Layer (Face) - Index 1
                val face = layer.getDrawable(1) as? android.graphics.drawable.GradientDrawable
                face?.let {
                    it.colors = intArrayOf(buttonStartColor, buttonEndColor)
                    it.orientation = android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM
                    // Add a subtle white stroke for pop
                    it.setStroke(3, androidx.core.graphics.ColorUtils.setAlphaComponent(android.graphics.Color.WHITE, 128))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun animateGreetingEmoji() {
        // Pivot at bottom center gives a better "wave" feel
        binding.tvGreetingEmoji.pivotX = binding.tvGreetingEmoji.width / 2f
        binding.tvGreetingEmoji.pivotY = binding.tvGreetingEmoji.height.toFloat()

        val scaleX = android.animation.PropertyValuesHolder.ofFloat(android.view.View.SCALE_X, 1.0f, 1.2f)
        val scaleY = android.animation.PropertyValuesHolder.ofFloat(android.view.View.SCALE_Y, 1.0f, 1.2f)
        
        // Let's keep it simple smooth shake
        val simpleRotate = android.animation.PropertyValuesHolder.ofFloat(android.view.View.ROTATION, -15f, 15f)

        val animator = android.animation.ObjectAnimator.ofPropertyValuesHolder(binding.tvGreetingEmoji, scaleX, scaleY, simpleRotate)
        animator.duration = 1200
        animator.repeatCount = android.animation.ObjectAnimator.INFINITE
        animator.repeatMode = android.animation.ObjectAnimator.REVERSE
        animator.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        animator.start()
    }

    override fun onResume() {
        super.onResume()
        fetchLatestBalance()
    }

    private fun fetchLatestBalance() {
        // Show cached first for immediate feedback
        updateBalance()

        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val serverBalance = document.getLong("mintBalance") ?: 0L
                    var activityDates = document.get("activityDates") as? List<Long> ?: emptyList()
                    
                    // MARK DAILY ATTENDANCE
                    val today = System.currentTimeMillis()
                    val isTodayRecorded = activityDates.any { StreakUtils.isToday(it) }
                    
                    if (!isTodayRecorded) {
                        val newDates = activityDates.toMutableList()
                        newDates.add(today)
                        activityDates = newDates
                        
                        db.collection("users").document(uid).update("activityDates", newDates)
                            .addOnSuccessListener {
                                // Updated successfully
                            }
                    }
                    
                    // Check if different from local
                    val sessionManager = com.digitar.mintx.utils.SessionManager(requireContext())
                    val localBalance = sessionManager.getMintBalance()
                    
                    if (serverBalance != localBalance) {
                        sessionManager.saveMintBalance(serverBalance)
                        // Animate from old local to new server
                        animateBalance(localBalance, serverBalance)
                    }
                    
                    updateStreakUI(activityDates)
                }
            }
            .addOnFailureListener {
                // If failed, we already showed cached via initial updateBalance()
            }
    }

    private fun updateStreakUI(activityDates: List<Long>) {
        // Stop loading animation
        isStreakLoading = false
        
        val currentWeekActivity = StreakUtils.getCurrentWeekActivity(activityDates)
        val currentStreak = StreakUtils.getCurrentStreak(activityDates)
        val streakContainer = binding.llStreakDots
        val childCount = streakContainer.childCount
        
        // 7 dots representing Mon-Sun of current week
        for (i in 0 until 7) {
            val viewIndex = i * 2 // 0, 2, 4, ...
            if (viewIndex >= childCount) break
            
            val dotView = streakContainer.getChildAt(viewIndex) as? android.widget.ImageView
            val isActive = currentWeekActivity[i]
            
            // Reset animation state to normal
            dotView?.apply {
                scaleX = 1f
                scaleY = 1f
                alpha = 1f
            }
            
            dotView?.setImageResource(if (isActive) R.drawable.streak_dot_active else R.drawable.streak_dot_inactive)
            
            // Connecting Lines
            if (i < 6) {
                val lineIndex = viewIndex + 1
                if (lineIndex < childCount) {
                    val lineView = streakContainer.getChildAt(lineIndex)
                    
                    // Connect active days
                    val nextActive = currentWeekActivity[i+1]
                    val isLineActive = isActive && nextActive
                    
                    lineView.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (isLineActive) R.color.quiz_orange else R.color.gray_500
                        )
                    )
                }
            }
        }
        
        // Update label
        val labelView = streakContainer.getChildAt(childCount - 1) as? android.widget.TextView
        val weekNum = if (currentStreak > 0) ((currentStreak - 1) / 7) + 1 else 1
        labelView?.text = "Week $weekNum ($currentStreak Days)"
    }

    private fun updateBalance() {
        val sessionManager = com.digitar.mintx.utils.SessionManager(requireContext())
        val newBalance = sessionManager.getMintBalance()
        
        // Animate Balance
        animateBalance(0, newBalance)
    }

    private fun animateBalance(from: Long, to: Long) {
        balanceAnimator?.cancel()
        balanceAnimator = android.animation.ValueAnimator.ofInt(from.toInt(), to.toInt())
        balanceAnimator?.apply {
            duration = 1500
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { animation ->
                 _binding?.tvBalanceAmount?.text = animation.animatedValue.toString()
            }
            start()
        }
    }

    private fun showQuizCategorySelector() {
        val bottomSheet = QuizCategoryBottomSheet.newInstance()
        bottomSheet.onQuizStarted = { categories ->
            // Pass categories via Intent
            val intent = Intent(requireContext(), QuizActivity::class.java)
            intent.putStringArrayListExtra("categories", ArrayList(categories))
            startActivity(intent)
        }
        bottomSheet.show(childFragmentManager, QuizCategoryBottomSheet.TAG)
    }

    private fun navigateToQuiz() {
        startActivity(Intent(requireContext(), QuizActivity::class.java))
    }
    
    fun animateStreakDotsLoader() {
        // Stop if data is loaded
        if (!isStreakLoading) return
        
        val dotIds = listOf(
            R.id.streak_dot_0,
            R.id.streak_dot_1,
            R.id.streak_dot_2,
            R.id.streak_dot_3,
            R.id.streak_dot_4,
            R.id.streak_dot_5,
            R.id.streak_dot_6
        )
        
        // Animate each dot sequentially
        dotIds.forEachIndexed { index, dotId ->
            val dot = binding.root.findViewById<android.widget.ImageView>(dotId)
            dot?.let {
                // Reset initial state
                it.scaleX = 1f
                it.scaleY = 1f
                it.alpha = 0.3f
                
                // Create sequential animation
                it.postDelayed({
                    // Scale up and fade in
                    it.animate()
                        .scaleX(1.4f)
                        .scaleY(1.4f)
                        .alpha(1f)
                        .setDuration(300)
                        .withEndAction {
                            // Scale back down
                            it.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .alpha(0.3f)
                                .setDuration(300)
                                .start()
                        }
                        .start()
                }, (index * 200).toLong()) // 200ms delay between each dot
            }
        }
        
        // Loop the animation only if still loading
        binding.root.postDelayed({
            if (_binding != null && isStreakLoading) {
                animateStreakDotsLoader()
            }
        }, (dotIds.size * 200 + 600).toLong()) // Restart after all dots finish
    }

    override fun onDestroyView() {
        super.onDestroyView()
        balanceAnimator?.cancel()
        _binding = null
    }
}