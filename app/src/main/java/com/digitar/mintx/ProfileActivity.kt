package com.digitar.mintx

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.digitar.mintx.auth.AuthActivity
import com.digitar.mintx.databinding.ActivityProfileViewBinding
import com.digitar.mintx.ui.OnboardingBottomSheetFragment
import com.digitar.mintx.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileViewBinding
    private lateinit var sessionManager: SessionManager

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Cache for category images
    private val categoryImageMap = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        window.statusBarColor = ContextCompat.getColor(this, R.color.mint_gold)
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val userName = sessionManager.getUserName() ?: "User"
        binding.tvUserName.text = userName
        binding.tvUserHandle.text = "@${userName.replace(" ", "").lowercase()}"
        
        binding.tvMobileNumber.text = "${sessionManager.getUserMobile()}"
        
        val age = sessionManager.getUserAge()
        binding.tvAge.text = "$age Years"
        
        binding.tvPoints.text = "${sessionManager.getMintBalance()}"
        
        fetchAllData()
    }
    
    private fun fetchAllData() {
        // 1. Fetch Categories metadata first (to get images)
        db.collection("quiz_categories").get()
            .addOnSuccessListener { result ->
                categoryImageMap.clear()
                for (doc in result) {
                    val name = doc.getString("name")
                    val url = doc.getString("imageUrl") ?: doc.getString("image")
                    if (name != null && url != null) {
                        categoryImageMap[name] = url
                    }
                }
                // 2. Then User Data
                fetchUserData()
            }
            .addOnFailureListener {
                // Return to fetching user data even if cats fail
                fetchUserData()
            }
    }
    
    private fun fetchUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(com.digitar.mintx.data.model.User::class.java)
                    user?.let {
                        binding.tvUserName.text = it.name
                        binding.tvUserHandle.text = "@${it.name.replace(" ", "").lowercase()}"
                        binding.tvAge.text = "${it.age} Years"
                        
                        binding.tvPoints.text = "${it.mintBalance}"
                        sessionManager.saveMintBalance(it.mintBalance)
                        
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
                                } else {
                                    // "otherwise, don’t display the image"
                                    chip.chipIcon = null
                                    chip.isChipIconVisible = false
                                }
                                
                                binding.chipGroupCategories.addView(chip)
                            }
                        } else {
                            val chip = com.google.android.material.chip.Chip(this@ProfileActivity)
                            chip.text = "None selected"
                            chip.isCheckable = false
                            chip.isClickable = false
                            chip.isChipIconVisible = false
                            binding.chipGroupCategories.addView(chip)
                        }
                    }
                }
            }
            .addOnFailureListener {
                // Ignore errors or show toast
            }
    }

    private fun setupListeners() {
        binding.cardLogout.setOnClickListener {
            sessionManager.logout()
            navigateToLogin()
        }
        
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.cardGamePreferences.setOnClickListener {
            val onboardingFragment = OnboardingBottomSheetFragment()
            onboardingFragment.show(supportFragmentManager, "OnboardingBottomSheet")
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
