package com.appslabs.mintx

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.appslabs.mintx.auth.AuthActivity
import com.appslabs.mintx.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val SPLASH_DELAY = 2500L // 2.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make full screen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startAnimations()
        
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DELAY)
    }

    private fun startAnimations() {
        // Initial state
        binding.ivAppLogo.alpha = 0f
        binding.ivAppLogo.scaleX = 0.8f
        binding.ivAppLogo.scaleY = 0.8f
        binding.ivAppLogo.translationY = 50f
        
        binding.tvAppName.alpha = 0f
        binding.tvAppName.translationY = 30f
        
        binding.viewGlow.alpha = 0f
        binding.viewGlow.scaleX = 0.5f
        binding.viewGlow.scaleY = 0.5f

        // Logo Animation
        binding.ivAppLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(1200)
            .setInterpolator(DecelerateInterpolator())
            .start()
            
        // Glow Animation (Slightly delayed)
        binding.viewGlow.animate()
            .alpha(0.6f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(1500)
            .setStartDelay(200)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Text Animation (Delayed)
        binding.tvAppName.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(1000)
            .setStartDelay(500)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun navigateToNextScreen() {
        // Check for auth state here if needed, for now defaulting to AuthActivity as entry
        // If you have a way to check if user is logged in, use it here.
        // For example: if (authManager.isLoggedIn()) MainActivity else AuthActivity
        
        // Assuming AuthActivity handles the routing to MainActivity if already logged in (based on typical flows)
        // Check Manifest: AuthActivity is typically the gatekeeper.
        
        val intent = Intent(this, MainActivity::class.java)
        // Or if you want to go to AuthActivity first:
        // val intent = Intent(this, AuthActivity::class.java)
        
        // NOTE: Based on existing manifest, MainActivity was LAUNCHER. 
        // Usually apps go Splash -> MainActivity (which checks auth) OR Splash -> Auth -> Main.
        // Let's assume MainActivity for now as it was the original Launcher.
        
        startActivity(intent)
        finish()
        
        // Smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

