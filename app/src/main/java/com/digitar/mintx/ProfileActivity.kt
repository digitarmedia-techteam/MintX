package com.digitar.mintx

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.digitar.mintx.databinding.ActivityProfileViewBinding
import com.digitar.mintx.ui.OnboardingBottomSheetFragment
import com.digitar.mintx.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileViewBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sessionManager: SessionManager

    private val categoryImageMap = mapOf(
        "science" to "https://cdn-icons-png.flaticon.com/512/3655/3655580.png",
        "history" to "https://cdn-icons-png.flaticon.com/512/3976/3976625.png"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color to match header
        window.statusBarColor = ContextCompat.getColor(this, R.color.mint_gold)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        sessionManager = SessionManager(this)

        setupUI()
        setupListeners()
        fetchAllData()
    }

    private fun setupUI() {
        // Set default values
        binding.tvUserName.text = "Loading..."
        binding.tvUserHandle.text = "@username"
        binding.tvAge.text = "00 Years"
        binding.tvPoints.text = "0"
        binding.tvXP.text = "0"
        binding.tvLevel.text = "1"
        binding.tvMobileNumber.text = auth.currentUser?.phoneNumber ?: "+91 0000 00000"
        
        // Set default progress
        binding.pbEasy.progress = 0
        binding.pbMedium.progress = 0
        binding.pbHard.progress = 0
        
        binding.tvServedEasy.text = "0"
        binding.tvServedMedium.text = "0"
        binding.tvServedHard.text = "0"
        
        // Set button colors
        binding.btnBack.setColorFilter(ContextCompat.getColor(this, R.color.fixed_white))
        binding.btnSettings.setColorFilter(ContextCompat.getColor(this, R.color.fixed_white))
    }

    private fun fetchAllData() {
        fetchUserData()
    }

    private fun fetchUserData() {
        val uid = auth.currentUser?.uid ?: return
        
        // Start loading state
        binding.viewSolvedStats.startLoading()
        
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(com.digitar.mintx.data.model.User::class.java)
                    user?.let {
                        binding.tvUserName.text = it.name
                        
                        // Email Logic from DB
                        if (it.email.isNotEmpty()) {
                             binding.tvUserHandle.text = it.email
                        } else {
                             binding.tvUserHandle.text = "@${it.name.replace(" ", "").lowercase()}"
                        }
                        
                        // Photo Logic from DB
                        // Photo Logic from DB
                        if (it.photoUrl.isNotEmpty()) {
                             Glide.with(this@ProfileActivity)
                                .load(it.photoUrl)
                                .circleCrop()
                                .placeholder(R.drawable.user_gif)
                                .error(R.drawable.user_gif)
                                .listener(object : com.bumptech.glide.request.RequestListener<Drawable> {
                                    override fun onLoadFailed(
                                        e: com.bumptech.glide.load.engine.GlideException?,
                                        model: Any?,
                                        target: com.bumptech.glide.request.target.Target<Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        showInitialsAvatar(it.name)
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: Drawable,
                                        model: Any,
                                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                                        dataSource: com.bumptech.glide.load.DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        binding.tvAvatarInitials.visibility = android.view.View.GONE
                                        binding.ivAvatar.visibility = android.view.View.VISIBLE
                                        return false
                                    }
                                })
                                .into(binding.ivAvatar)
                        } else {
                            showInitialsAvatar(it.name)
                        }

                        binding.tvAge.text = "${it.age} Years"
                        
                        // Points/Mint Balance (Coins)
                        binding.tvPoints.text = "${it.mintBalance}"
                        sessionManager.saveMintBalance(it.mintBalance)
                        
                        // Update Contribution Graph
                        try {
                            // Uses dailyStats map now
                            binding.viewContributionGraph.setActivityData(it.dailyStats)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        
                        // Solved Stats
                        // Fetch Total Questions Count asynchronously
                        db.collection("questions").get().addOnSuccessListener { questionsSnapshot ->
                             val totalQuestions = questionsSnapshot.size()
                             binding.viewSolvedStats.setData(it.solvedEasy, it.solvedMedium, it.solvedHard, totalQuestions)
                             
                             // Animate progress bars
                             animateProgressBar(binding.pbEasy, it.solvedEasy, 50)
                             animateProgressBar(binding.pbMedium, it.solvedMedium, 30)
                             animateProgressBar(binding.pbHard, it.solvedHard, 20)
                        }.addOnFailureListener { _ ->
                             // Fallback if fails
                             binding.viewSolvedStats.setData(it.solvedEasy, it.solvedMedium, it.solvedHard, 0)
                             
                             // Animate progress bars
                             animateProgressBar(binding.pbEasy, it.solvedEasy, 50)
                             animateProgressBar(binding.pbMedium, it.solvedMedium, 30)
                             animateProgressBar(binding.pbHard, it.solvedHard, 20)
                        }
                        
                        binding.tvServedEasy.text = "${it.solvedEasy}"
                        binding.tvServedMedium.text = "${it.solvedMedium}"
                        binding.tvServedHard.text = "${it.solvedHard}"
                        
                        // Level and XP
                        binding.tvXP.text = "${it.totalXP}"
                        
                        val levelInfo = com.digitar.mintx.utils.LevelUtils.calculateLevelInfo(it.totalXP)
                        binding.tvLevel.text = "${levelInfo.level}"
                        
                        // Update categories
                        binding.chipGroupCategories.removeAllViews()
                        if (it.categories.isNotEmpty()) {
                            it.categories.forEach { catName ->
                                val chip = com.google.android.material.chip.Chip(this@ProfileActivity)
                                chip.text = catName.replaceFirstChar { char -> char.uppercase() }
                                chip.isCheckable = false
                                chip.isClickable = false
                                
                                // Image Logic
                                val imageUrl = categoryImageMap[catName]
                                if (!imageUrl.isNullOrEmpty()) {
                                    // Load with Glide
                                    Glide.with(this@ProfileActivity)
                                        .asDrawable()
                                        .load(imageUrl)
                                        .circleCrop()
                                        .into(object : CustomTarget<Drawable>() {
                                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                                chip.chipIcon = resource
                                                chip.isChipIconVisible = true
                                            }
                                            override fun onLoadCleared(placeholder: Drawable?) {
                                                // Do nothing
                                            }
                                        })
                                }
                                
                                binding.chipGroupCategories.addView(chip)
                            }
                        } else {
                            val chip = com.google.android.material.chip.Chip(this@ProfileActivity)
                            chip.text = "No categories selected"
                            chip.isCheckable = false
                            chip.isClickable = false
                            binding.chipGroupCategories.addView(chip)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSettings.setOnClickListener {
            // Navigate to settings
        }

        binding.cardLogout.setOnClickListener {
            auth.signOut()
            sessionManager.logout()
            navigateToLogin()
        }

        binding.cardGamePreferences.setOnClickListener {
            val onboardingFragment = OnboardingBottomSheetFragment()
            onboardingFragment.show(supportFragmentManager, "OnboardingBottomSheet")
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, com.digitar.mintx.auth.AuthActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
    
    private fun showInitialsAvatar(userName: String?) {
        if (userName.isNullOrEmpty()) {
            binding.tvAvatarInitials.visibility = android.view.View.GONE
            binding.ivAvatar.visibility = android.view.View.VISIBLE
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
        binding.ivAvatar.visibility = android.view.View.GONE
        binding.tvAvatarInitials.visibility = android.view.View.VISIBLE
    }
    
    private fun animateProgressBar(progressBar: android.widget.ProgressBar, targetProgress: Int, max: Int) {
        progressBar.max = max
        progressBar.progress = 0
        
        val animator = android.animation.ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress)
        animator.duration = 1000 // 1 second animation
        animator.interpolator = android.view.animation.DecelerateInterpolator()
        animator.start()
    }
}
