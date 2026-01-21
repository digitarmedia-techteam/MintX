package com.digitar.mintx

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.digitar.mintx.auth.AuthActivity
import com.digitar.mintx.databinding.ActivityProfileViewBinding
import com.digitar.mintx.utils.SessionManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileViewBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

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
        
        // Random stats for premium feel
        binding.tvPoints.text = "2.4k"
    }

    private fun setupListeners() {
        binding.cardLogout.setOnClickListener {
            sessionManager.logout()
            navigateToLogin()
        }
        
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
