package com.digitar.mintx

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.digitar.mintx.auth.AuthActivity
import com.digitar.mintx.databinding.ActivityProfileViewBinding
import com.digitar.mintx.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.digitar.mintx.ui.OnboardingBottomSheetFragment

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileViewBinding
    private lateinit var sessionManager: SessionManager

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
        
        fetchUserData()
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
                            it.categories.forEach { cat ->
                                val chip = com.google.android.material.chip.Chip(this@ProfileActivity)
                                chip.text = cat.replaceFirstChar { char -> char.uppercase() }
                                chip.isCheckable = false
                                chip.isClickable = false
                                binding.chipGroupCategories.addView(chip)
                            }
                        } else {
                            val chip = com.google.android.material.chip.Chip(this@ProfileActivity)
                            chip.text = "None selected"
                            chip.isCheckable = false
                            chip.isClickable = false
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
