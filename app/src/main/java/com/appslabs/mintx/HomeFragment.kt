package com.appslabs.mintx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.appslabs.mintx.databinding.FragmentHomeBinding
import com.appslabs.mintx.ui.quiz.QuizCategoryBottomSheet
import android.content.Intent
import java.util.ArrayList
import androidx.core.content.ContextCompat
import com.appslabs.mintx.utils.StreakUtils
import com.appslabs.mintx.RewardsStoreActivity
import com.appslabs.mintx.ProfileActivity
import com.appslabs.mintx.QuizActivity
import com.appslabs.mintx.R

class HomeFragment : Fragment() {
    private var balanceAnimator: android.animation.ValueAnimator? = null
    private var waveAnimator: android.animation.AnimatorSet? = null
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
        
        // Setup Profile Click for initials overlay
        binding.tvAvatarInitials.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }
        
        // Setup Predict & Win button with cricket ball animation
        binding.btnPredictWin.setOnClickListener { view ->
            // Spring bounce animation - stays in place, no elevation changes
            view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction {
                    // Bounce back with overshoot for spring effect
                    view.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(150)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .withEndAction {
                            // Settle back to normal
                            view.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .setInterpolator(android.view.animation.LinearInterpolator())
                                .start()
                        }
                        .start()
                }
                .start()
            
            // Trigger cricket ball animation and dialog
            animateCricketBall()
            showComingSoonDialog()
        }
        
        // Show basic user data
        val sessionManager = com.appslabs.mintx.utils.SessionManager(requireContext())
        val name = sessionManager.getUserName()
        if (name != null) {
            binding.tvGreeting.text = "Hello, $name"
            animateGreetingEmoji()
        }

        // Avatar Logic
        val photoUrl = sessionManager.getUserPhoto()
        if (!photoUrl.isNullOrEmpty()) {
            // Load profile image
            com.bumptech.glide.Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.user_gif)
                .error(R.drawable.user_gif)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Show initials if image loading fails
                        showInitialsAvatar(name)
                        binding.navHeaderAvatar.visibility = android.view.View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Hide initials when image loads successfully
                        binding.tvAvatarInitials.visibility = android.view.View.GONE
                        binding.navHeaderAvatar.visibility = android.view.View.VISIBLE
                        return false
                    }
                })
                .into(binding.navHeaderAvatar)
        } else {
            // No photo URL - show initials
            showInitialsAvatar(name)
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
        // Pivot at bottom center (wrist position) for natural waving
        binding.tvGreetingEmoji.post {
            binding.tvGreetingEmoji.pivotX = binding.tvGreetingEmoji.width / 2f
            binding.tvGreetingEmoji.pivotY = binding.tvGreetingEmoji.height.toFloat()
            
            // Create a natural waving animation
            performWaveAnimation()
        }
    }
    
    private fun performWaveAnimation() {
        // Cancel any existing animation
        waveAnimator?.cancel()
        
        // Natural hand wave: quick back-and-forth rotations, then pause
        waveAnimator = android.animation.AnimatorSet()
        
        // Wave cycle: 0° -> 20° -> -20° -> 20° -> -20° -> 20° -> 0°
        // This creates 3 quick waves
        val wave = android.animation.ObjectAnimator.ofFloat(
            binding.tvGreetingEmoji,
            "rotation",
            0f, 20f, -20f, 20f, -20f, 20f, 0f
        ).apply {
            duration = 600 // Total wave duration: 600ms (quick waving)
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        }
        
        // Small scale pulse on first wave for emphasis (optional)
        val scaleUpX = android.animation.ObjectAnimator.ofFloat(
            binding.tvGreetingEmoji,
            "scaleX",
            1f, 1.1f
        ).apply {
            duration = 100
            interpolator = android.view.animation.DecelerateInterpolator()
        }
        
        val scaleDownX = android.animation.ObjectAnimator.ofFloat(
            binding.tvGreetingEmoji,
            "scaleX",
            1.1f, 1f
        ).apply {
            duration = 100
            interpolator = android.view.animation.AccelerateInterpolator()
        }
        
        val scaleUpY = android.animation.ObjectAnimator.ofFloat(
            binding.tvGreetingEmoji,
            "scaleY",
            1f, 1.1f
        ).apply {
            duration = 100
            interpolator = android.view.animation.DecelerateInterpolator()
        }
        
        val scaleDownY = android.animation.ObjectAnimator.ofFloat(
            binding.tvGreetingEmoji,
            "scaleY",
            1.1f, 1f
        ).apply {
            duration = 100
            interpolator = android.view.animation.AccelerateInterpolator()
        }
        
        // Combine scale animations
        val scaleUp = android.animation.AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY)
        }
        
        val scaleDown = android.animation.AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY)
        }
        
        // Play wave with subtle scale
        waveAnimator?.apply {
            playSequentially(scaleUp, scaleDown)
            playTogether(wave)
            
            // Add a listener to repeat after pause
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // Pause for 2 seconds, then wave again
                    _binding?.tvGreetingEmoji?.postDelayed({
                        if (_binding != null) {
                            performWaveAnimation()
                        }
                    }, 2000) // 2 second pause between wave cycles
                }
            })
            
            start()
        }
    }


    private fun showInitialsAvatar(userName: String?) {
        if (userName.isNullOrEmpty()) {
            // If no name, just hide the avatar image and show default
            binding.navHeaderAvatar.visibility = android.view.View.GONE
            binding.tvAvatarInitials.visibility = android.view.View.GONE
            return
        }
        
        // Extract initials: first letter of first name + first letter of last name
        val nameParts = userName.trim().split(" ")
        val initials = when {
            nameParts.size >= 2 -> {
                // First name initial + Last name initial
                "${nameParts.first().firstOrNull()?.uppercaseChar() ?: ""}${nameParts.last().firstOrNull()?.uppercaseChar() ?: ""}"
            }
            nameParts.size == 1 -> {
                // Only one name - take first two letters or just first letter
                val name = nameParts.first()
                if (name.length >= 2) {
                    "${name[0].uppercaseChar()}${name[1].uppercaseChar()}"
                } else {
                    "${name.firstOrNull()?.uppercaseChar() ?: ""}"
                }
            }
            else -> "?"
        }
        
        // Set initials text
        binding.tvAvatarInitials.text = initials
        
        // Hide the image avatar and show initials
        binding.navHeaderAvatar.visibility = android.view.View.GONE
        binding.tvAvatarInitials.visibility = android.view.View.VISIBLE
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
                if (!isAdded || context == null) return@addOnSuccessListener

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
                    val sessionManager = com.appslabs.mintx.utils.SessionManager(requireContext())
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
        val sessionManager = com.appslabs.mintx.utils.SessionManager(requireContext())
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


    private fun animateCricketBall() {
        val cricketBall = binding.root.findViewById<android.widget.ImageView>(R.id.iv_cricket_ball)
        cricketBall?.let { ball ->
            // Create a realistic cricket ball bounce animation with spin
            
            // First bounce
            val bounce1 = android.animation.ObjectAnimator.ofFloat(ball, "translationY", 0f, -120f, 0f).apply {
                duration = 400
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            }
            
            // Second bounce (smaller)
            val bounce2 = android.animation.ObjectAnimator.ofFloat(ball, "translationY", 0f, -80f, 0f).apply {
                duration = 350
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            }
            
            // Third bounce (smallest)
            val bounce3 = android.animation.ObjectAnimator.ofFloat(ball, "translationY", 0f, -40f, 0f).apply {
                duration = 300
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            }
            
            // Continuous rotation for spinning effect
            val spin = android.animation.ObjectAnimator.ofFloat(ball, "rotation", 0f, 1080f).apply {
                duration = 1050 // Total duration of all bounces
                interpolator = android.view.animation.LinearInterpolator()
            }
            
            // Scale effect for depth perception
            val scaleX = android.animation.ObjectAnimator.ofFloat(ball, "scaleX", 1f, 1.4f, 1f, 1.3f, 1f, 1.2f, 1f).apply {
                duration = 1050
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            }
            
            val scaleY = android.animation.ObjectAnimator.ofFloat(ball, "scaleY", 1f, 1.4f, 1f, 1.3f, 1f, 1.2f, 1f).apply {
                duration = 1050
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            }
            
            // Sequence the bounces
            val bounceSequence = android.animation.AnimatorSet().apply {
                playSequentially(bounce1, bounce2, bounce3)
            }
            
            // Play all animations together
            android.animation.AnimatorSet().apply {
                playTogether(bounceSequence, spin, scaleX, scaleY)
                start()
            }
        }
    }
    
    private fun showComingSoonDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_coming_soon, null)
        
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Make dialog background transparent
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        
        // Animate the cricket ball icon in the dialog
        val dialogBall = dialogView.findViewById<android.widget.ImageView>(R.id.iv_cricket_ball_dialog)
        dialogBall?.let { ball ->
            // Cute continuous rotation
            ball.animate()
                .rotation(360f)
                .setDuration(2000)
                .setInterpolator(android.view.animation.LinearInterpolator())
                .withEndAction {
                    // Loop the rotation
                    ball.rotation = 0f
                    ball.animate()
                        .rotation(360f)
                        .setDuration(2000)
                        .setInterpolator(android.view.animation.LinearInterpolator())
                        .start()
                }
                .start()
            
            // Add a subtle scale pulse
            ball.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(800)
                .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction {
                    ball.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(800)
                        .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                        .start()
                }
                .start()
        }
        
        // Setup OK button
        val btnOk = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_ok)
        btnOk?.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
        
        // Animate dialog entrance
        dialogView.startAnimation(
            android.view.animation.AnimationUtils.loadAnimation(
                requireContext(),
                R.anim.coming_soon_enter
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        balanceAnimator?.cancel()
        waveAnimator?.cancel()
        _binding = null
    }
}
