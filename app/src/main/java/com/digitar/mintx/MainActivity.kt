package com.digitar.mintx

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.digitar.mintx.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sessionManager = com.digitar.mintx.utils.SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            val intent = android.content.Intent(this, com.digitar.mintx.auth.AuthActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // 1. Enable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.bottomNavigation.selectedItemId != R.id.navigation_home) {
                    binding.bottomNavigation.selectedItemId = R.id.navigation_home
                } else {
                    showExitDialog()
                }
            }
        })

        // 2. Set System Bar Icon Colors - Handled by Theme (values/themes.xml)
        // WindowInsetsControllerCompat(window, binding.root) // Not needed if XML handles it

        // 3. Handle Insets to prevent UI overlap (StatusBar and NavigationBar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply top padding to the root to avoid status bar overlap
            // This ensures content starts below status bar while background stays visible
            v.setPadding(0, systemBars.top, 0, 0)
            
            // Apply bottom padding to Bottom Navigation to avoid gesture bar overlap
            // We use padding on the view itself so it draws correctly above the system nav
            binding.bottomNavigation.post {
                binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)
            }
            
            insets
        }

        setupNavigation()

        // 4. Load default fragment or handle intent
        if (savedInstanceState == null) {
            if (intent.hasExtra("NAV_ID")) {
                 handleIntent(intent)
            } else {
                 loadFragment(HomeFragment())
            }

            // 5. Check for Onboarding (Categories)
            if (!sessionManager.isCategoriesSelected()) {
                val onboardingFragment = com.digitar.mintx.ui.OnboardingBottomSheetFragment()
                onboardingFragment.show(supportFragmentManager, "OnboardingBottomSheet")
            }
        }
    }
    
    // Note: If using Navigation Drawer, we would set up the listener here.
    // Since we use Bottom Navigation and likely have a Header in HomeFragment or a Toolbar,
    // we need to handle the click there.
    // However, if the user specifically asked for "Nav Header Avatar" in "Navigation Drawer",
    // maybe I should add a DrawerLayout? 
    // BUT the current design is Bottom Nav. 
    // I will assume the user refers to the Profile Icon in the Home Screen.
    // I will check HomeFragment next.

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.navigation_quiz -> {
                    startActivity(android.content.Intent(this, QuizActivity::class.java))
                    false
                }
                R.id.navigation_prediction -> {
                    loadFragment(PredictionFragment())
                    true
                }
                R.id.navigation_earn -> {
                    loadFragment(OffersFragment())
                    true
                }
                R.id.navigation_wallet -> {
                    loadFragment(WalletFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent) {
        if (intent.hasExtra("NAV_ID")) {
            val navId = intent.getIntExtra("NAV_ID", R.id.navigation_home)
            binding.bottomNavigation.selectedItemId = navId
        }
    }

    private fun showExitDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}