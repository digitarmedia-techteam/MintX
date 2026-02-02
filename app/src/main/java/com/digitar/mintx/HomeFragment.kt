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

class HomeFragment : Fragment() {
    private var balanceAnimator: android.animation.ValueAnimator? = null
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
//            showQuizCategorySelector()
            navigateToQuiz()
        }

        binding.btnRedeemHome.setOnClickListener {
            startActivity(Intent(requireContext(), RewardsStoreActivity::class.java))
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
        // Load Live GIF
        try {
            com.bumptech.glide.Glide.with(this)
                .load(R.drawable.live)
                .into(binding.ivLiveBadge)
        } catch (e: Exception) {
            // Fallback
        }
        setupFeaturedCard("")
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
                    
                    // Check if different from local
                    val sessionManager = com.digitar.mintx.utils.SessionManager(requireContext())
                    val localBalance = sessionManager.getMintBalance()
                    
                    if (serverBalance != localBalance) {
                        sessionManager.saveMintBalance(serverBalance)
                        // Animate from old local to new server
                        animateBalance(localBalance, serverBalance)
                    }
                }
            }
            .addOnFailureListener {
                // If failed, we already showed cached via initial updateBalance()
            }
    }

    private fun updateBalance() {
        val sessionManager = com.digitar.mintx.utils.SessionManager(requireContext())
        val newBalance = sessionManager.getMintBalance()
        
        // Animate Balance
        // We assume starting from 0 if it's the first load, or we could track previous. 
        // For "spinning loader" effect, staring from 0 or a low number is good visuals on valid screens.
        // Or we just animate from 0 every time onResume for the "effect".
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

    override fun onDestroyView() {
        super.onDestroyView()
        balanceAnimator?.cancel()
        _binding = null
    }
}