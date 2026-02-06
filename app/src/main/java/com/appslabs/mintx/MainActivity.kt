package com.appslabs.mintx

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.appslabs.mintx.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.appslabs.mintx.utils.SessionManager
import com.appslabs.mintx.auth.AuthActivity
import com.appslabs.mintx.ui.OnboardingBottomSheetFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentFragmentTag: String? = null
    private var currentTabIndex = 0
    private val appUpdateManager by lazy { com.google.android.play.core.appupdate.AppUpdateManagerFactory.create(this) }
    private val UPDATE_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check for updates immediately
        checkForAppUpdate()

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
            
            // Check if user needs to set their Date of Birth
            checkAndPromptForDOB()
        }
    }
    
    private fun checkAndPromptForDOB() {
        val sessionManager = SessionManager(this)
        val userAge = sessionManager.getUserAge()
        
        // Show DOB dialog if age is not set or invalid (0 or outside 13-80)
        if (userAge == 0 || userAge < 13 || userAge > 80) {
            showDOBDialog()
        }
    }
    
    private fun showDOBDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_dob_picker, null)
        val datePicker = dialogView.findViewById<android.widget.DatePicker>(R.id.datePicker)
        
        // Set maximum date to today
        datePicker.maxDate = System.currentTimeMillis()
        
        // Set minimum date to 130 years ago (reasonable limit)
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.YEAR, -130)
        datePicker.minDate = calendar.timeInMillis
        
        // Set initial date to 18 years ago (reasonable default)
        val defaultCalendar = java.util.Calendar.getInstance()
        defaultCalendar.add(java.util.Calendar.YEAR, -18)
        datePicker.init(
            defaultCalendar.get(java.util.Calendar.YEAR),
            defaultCalendar.get(java.util.Calendar.MONTH),
            defaultCalendar.get(java.util.Calendar.DAY_OF_MONTH),
            null
        )
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Set Your Date of Birth")
            .setMessage("Please select your date of birth to continue using the app.")
            .setView(dialogView)
            .setCancelable(false) // Non-dismissible
            .setPositiveButton("Confirm") { _, _ ->
                val day = datePicker.dayOfMonth
                val month = datePicker.month
                val year = datePicker.year
                
                val age = calculateAge(year, month, day)
                
                if (age < 13 || age > 80) {
                    android.widget.Toast.makeText(
                        this,
                        "Age must be between 13 and 80 years",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    // Show dialog again
                    showDOBDialog()
                } else {
                    // Save age to Firestore
                    saveAgeToFirestore(age)
                }
            }
            .create()
        
        dialog.show()
    }
    
    private fun calculateAge(year: Int, month: Int, day: Int): Int {
        val dob = java.util.Calendar.getInstance()
        dob.set(year, month, day)
        
        val today = java.util.Calendar.getInstance()
        
        var age = today.get(java.util.Calendar.YEAR) - dob.get(java.util.Calendar.YEAR)
        
        // Check if birthday hasn't occurred yet this year
        if (today.get(java.util.Calendar.DAY_OF_YEAR) < dob.get(java.util.Calendar.DAY_OF_YEAR)) {
            age--
        }
        
        return age
    }
    
    private fun saveAgeToFirestore(age: Int) {
        val sessionManager = SessionManager(this)
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val userName = sessionManager.getUserName() ?: "User" // Handle nullable
        
        if (uid == null) {
            android.widget.Toast.makeText(this, "Failed to update age. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading
        val progressDialog = android.app.ProgressDialog(this).apply {
            setMessage("Updating your profile...")
            setCancelable(false)
            show()
        }
        
        // Use lifecycleScope for proper coroutine management
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = com.appslabs.mintx.auth.AuthRepository()
            val result = repository.updateUserProfile(uid, userName, age)
            
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                
                result.onSuccess {
                    // Update session
                    sessionManager.updateAge(age)
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Age updated successfully!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }.onFailure {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Failed to update age: ${it.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    // Show dialog again on failure
                    showDOBDialog()
                }
            }
        }
    }
    
    private fun checkForAppUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // Register listener for download progress
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    private val installStateUpdatedListener = com.google.android.play.core.install.InstallStateUpdatedListener { state ->
        if (state.installStatus() == com.google.android.play.core.install.model.InstallStatus.DOWNLOADED) {
            popupSnackbarForCompleteUpdate()
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        com.google.android.material.snackbar.Snackbar.make(
            binding.main,
            "An update has just been downloaded.",
            com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("RESTART") { appUpdateManager.completeUpdate() }
            setActionTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.mint_green))
            show()
        }
    }

    override fun onResume() {
        super.onResume()
        
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == com.google.android.play.core.install.model.InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                // Update flow failed or was canceled
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
