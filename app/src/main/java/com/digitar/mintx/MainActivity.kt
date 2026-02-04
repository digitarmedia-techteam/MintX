package com.digitar.mintx

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.digitar.mintx.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.digitar.mintx.utils.SessionManager
import com.digitar.mintx.auth.AuthActivity
import com.digitar.mintx.ui.OnboardingBottomSheetFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentFragmentTag: String? = null
    private var currentTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.bottomNavigation.selectedItemId != R.id.navigation_home) {
                    loadHome()
                } else {
                    showExitDialog()
                }
            }
        })

        // Optimized Insets Handling
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            
            // Pad the bottom navigation to be above system gesture bar
            // We pad the bottomNavigation view itself, enabling the glass effect to extend if desired,
            // or simply pushing the content up.
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupNavigation()

        if (savedInstanceState == null) {
            if (intent.hasExtra("NAV_ID")) {
                handleIntent(intent)
            } else {
                loadFragment(HomeFragment(), "HOME")
            }

            if (!sessionManager.isCategoriesSelected()) {
                val onboardingFragment = OnboardingBottomSheetFragment()
                onboardingFragment.show(supportFragmentManager, "OnboardingBottomSheet")
            }
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { index ->
            if (index == currentTabIndex && index != 1) { // Allow Quiz restart
                 return@setOnItemSelectedListener
            }

            val itemId = binding.bottomNavigation.getMenuIdForIndex(index)
            currentTabIndex = index

            when (itemId) {
                R.id.navigation_home -> loadFragment(HomeFragment(), "HOME")
                R.id.navigation_prediction -> loadFragment(PredictionFragment(), "PREDICT")
                R.id.navigation_earn -> loadFragment(OffersFragment(), "EARN")
                R.id.navigation_wallet -> loadFragment(WalletFragment(), "WALLET")
                R.id.navigation_quiz -> {
                    // Delay to finish Bottom Nav animation first (50ms optimized)
                    binding.bottomNavigation.postDelayed({
                        val intent = Intent(this, QuizActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        
                        val options = ActivityOptions.makeCustomAnimation(
                            this, 
                            R.anim.fade_in, 
                            R.anim.fade_out
                        )
                        startActivity(intent, options.toBundle())
                    }, 50)
                }
            }
        }
    }

    private fun loadFragment(fragment: Fragment, tag: String) {
        if (currentFragmentTag == tag) return

        val transaction = supportFragmentManager.beginTransaction()
        
        transaction.setCustomAnimations(
            R.anim.fade_in, 
            R.anim.fade_out,
            R.anim.fade_in, 
            R.anim.fade_out
        )

        transaction.replace(R.id.nav_host_fragment, fragment, tag)
        transaction.commit()

        currentFragmentTag = tag
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra("NAV_ID")) {
            val navId = intent.getIntExtra("NAV_ID", R.id.navigation_home)
            val index = binding.bottomNavigation.getIndexForMenuId(navId)

            currentTabIndex = index
            // Force select item ensures indicator moves correctly even if re-entering
            binding.bottomNavigation.selectItem(index, animate = false)

            when (navId) {
                R.id.navigation_home -> loadFragment(HomeFragment(), "HOME")
                R.id.navigation_prediction -> loadFragment(PredictionFragment(), "PREDICT")
                R.id.navigation_earn -> loadFragment(OffersFragment(), "EARN")
                R.id.navigation_wallet -> loadFragment(WalletFragment(), "WALLET")
                R.id.navigation_quiz -> {
                   val intentQuiz = Intent(this, QuizActivity::class.java)
                   intentQuiz.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                   val options = ActivityOptions.makeCustomAnimation(
                       this, 
                       R.anim.fade_in, 
                       R.anim.fade_out
                   )
                   startActivity(intentQuiz, options.toBundle())
                }
            }
        }
    }

    private fun loadHome() {
        val homeIndex = binding.bottomNavigation.getIndexForMenuId(R.id.navigation_home)
        binding.bottomNavigation.selectItem(homeIndex)
        loadFragment(HomeFragment(), "HOME")
        currentTabIndex = homeIndex
    }

    private fun showExitDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }
}