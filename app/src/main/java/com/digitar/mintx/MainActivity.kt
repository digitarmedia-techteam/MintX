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
        
        // 1. Enable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Set System Bar Icon Colors (Light icons for dark UI)
        val controller = WindowInsetsControllerCompat(window, binding.root)
        controller.isAppearanceLightStatusBars = false // false = white icons
        controller.isAppearanceLightNavigationBars = false // false = white icons

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

        // 4. Load default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.navigation_quiz -> {
                    loadFragment(QuizFragment())
                    true
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
}