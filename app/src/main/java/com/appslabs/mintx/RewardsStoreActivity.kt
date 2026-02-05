package com.appslabs.mintx

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.appslabs.mintx.databinding.ActivityRewardsStoreBinding
import com.appslabs.mintx.databinding.DialogRewardDetailsBinding
import com.appslabs.mintx.model.Redemption
import com.appslabs.mintx.model.Reward
import com.appslabs.mintx.ui.adapter.RedemptionAdapter
import com.appslabs.mintx.ui.adapter.RewardAdapter
import com.appslabs.mintx.ui.viewmodel.RewardsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth

class RewardsStoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardsStoreBinding
    private lateinit var viewModel: RewardsViewModel
    private lateinit var rewardAdapter: RewardAdapter
    private lateinit var redemptionAdapter: RedemptionAdapter
    
    private var selectedReward: Reward? = null
    private var currentTab = TAB_AVAILABLE
    
    companion object {
        private const val TAB_AVAILABLE = 0
        private const val TAB_PENDING = 1
        private const val TAB_COMPLETED = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return RewardsViewModel(applicationContext) as T
                }
            }
        )[RewardsViewModel::class.java]

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        loadInitialData()
    }

    private fun setupRecyclerView() {
        // Setup rewards adapter (for Available tab)
        rewardAdapter = RewardAdapter(
            rewards = emptyList(),
            onRewardClick = { reward -> onRewardSelected(reward) },
            onInfoClick = { reward -> showRewardDetails(reward) }
        )
        
        // Setup redemption adapter (for Pending/Completed tabs)
        redemptionAdapter = RedemptionAdapter(emptyList())
        
        // Initially show rewards adapter
        binding.rvRewards.layoutManager = GridLayoutManager(this, 2)
        binding.rvRewards.adapter = rewardAdapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnFooterContainer.setOnClickListener {
            selectedReward?.let { reward ->
                showConfirmationDialog(reward)
            } ?: run {
                Toast.makeText(this, "Please select a reward", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnTabAvailable.setOnClickListener { switchTab(TAB_AVAILABLE) }
        binding.btnTabPending.setOnClickListener { switchTab(TAB_PENDING) }
        binding.btnTabCompleted.setOnClickListener { switchTab(TAB_COMPLETED) }
    }

    private fun observeViewModel() {
        // Observe rewards
        viewModel.rewards.observe(this) { rewards ->
            android.util.Log.d("RewardsStore", "Rewards received: ${rewards.size}")
            rewardAdapter.updateData(rewards)
            
            if (currentTab == TAB_AVAILABLE) {
                updateUiState(isLoading = false, isEmpty = rewards.isEmpty(), isGrid = true)
            }
        }
        
        // Observe balance
        viewModel.balance.observe(this) { balance ->
            android.util.Log.d("RewardsStore", "Balance updated: $balance")
            binding.tvWalletBalance.text = String.format("%,d", balance)
        }
        
        // Observe pending redemptions
        viewModel.pendingRedemptions.observe(this) { redemptions ->
            android.util.Log.d("RewardsStore", "Pending redemptions: ${redemptions.size}")
            if (currentTab == TAB_PENDING) {
                redemptionAdapter.updateData(redemptions)
                updateUiState(isLoading = false, isEmpty = redemptions.isEmpty(), isGrid = false)
            }
        }
        
        // Observe completed redemptions
        viewModel.completedRedemptions.observe(this) { redemptions ->
            android.util.Log.d("RewardsStore", "Completed redemptions: ${redemptions.size}")
            if (currentTab == TAB_COMPLETED) {
                redemptionAdapter.updateData(redemptions)
                updateUiState(isLoading = false, isEmpty = redemptions.isEmpty(), isGrid = false)
            }
        }
        
        // Observe loading state
        viewModel.loading.observe(this) { isLoading ->
            android.util.Log.d("RewardsStore", "Loading: $isLoading")
            if (isLoading) {
                updateUiState(isLoading = true, isEmpty = false, isGrid = (currentTab == TAB_AVAILABLE))
            }
        }
        
        // Observe errors
        viewModel.error.observe(this) { error ->
            error?.let {
                android.util.Log.e("RewardsStore", "Error: $it")
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
                // If error occurs during loading, stop shimmer
                updateUiState(isLoading = false, isEmpty = true, isGrid = (currentTab == TAB_AVAILABLE))
            }
        }
        
        // Observe redemption success
        viewModel.redemptionSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Redemption request submitted successfully!", Toast.LENGTH_LONG).show()
                selectedReward = null
                resetFooterButton()
                
                // Switch to pending tab to show the request
                switchTab(TAB_PENDING)
            }
        }
    }

    private fun updateUiState(isLoading: Boolean, isEmpty: Boolean, isGrid: Boolean) {
        if (isLoading) {
            // SHOW SHIMMER
            binding.rvRewards.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.GONE
            
            if (isGrid) {
                binding.shimmerViewRewards.root.visibility = View.VISIBLE
                binding.shimmerViewRedemptions.root.visibility = View.GONE
            } else {
                binding.shimmerViewRewards.root.visibility = View.GONE
                binding.shimmerViewRedemptions.root.visibility = View.VISIBLE
            }
            return
        }
        
        // HIDE SHIMMER
        binding.shimmerViewRewards.root.visibility = View.GONE
        binding.shimmerViewRedemptions.root.visibility = View.GONE
        
        if (isEmpty) {
            // SHOW EMPTY STATE
            binding.rvRewards.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            
            val emptyMessage = when (currentTab) {
                TAB_AVAILABLE -> "No rewards available yet"
                TAB_PENDING -> "No pending requests"
                TAB_COMPLETED -> "No completed redemptions"
                else -> "No items found"
            }
            binding.tvEmptyMessage.text = emptyMessage
        } else {
            // SHOW CONTENT
            binding.rvRewards.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun loadInitialData() {
        viewModel.loadRewards()
        viewModel.loadBalance()
    }

    private fun switchTab(tab: Int) {
        currentTab = tab
        
        val tabs = listOf(binding.btnTabAvailable, binding.btnTabPending, binding.btnTabCompleted)
        tabs.forEachIndexed { index, textView ->
            textView.setBackgroundResource(
                if (index == tab) R.drawable.tab_pill_active else R.drawable.tab_pill_inactive
            )
            (textView as TextView).setTextColor(
                if (index == tab) 
                    androidx.core.content.ContextCompat.getColor(this, R.color.text_inverse)
                else 
                    androidx.core.content.ContextCompat.getColor(this, R.color.text_subtext)
            )
        }
        
        // Clear adapter data immediately to avoid flickering old data
        if (tab == TAB_AVAILABLE) {
            rewardAdapter.updateData(emptyList())
        } else {
            redemptionAdapter.updateData(emptyList())
        }
        
        // Show loading state immediately
        updateUiState(isLoading = true, isEmpty = false, isGrid = (tab == TAB_AVAILABLE))
        
        when (tab) {
            TAB_AVAILABLE -> {
                binding.rvRewards.layoutManager = GridLayoutManager(this, 2)
                binding.rvRewards.adapter = rewardAdapter
                binding.btnFooterContainer.visibility = View.VISIBLE
                resetFooterButton()
                viewModel.loadRewards()
                viewModel.loadBalance()
            }
            TAB_PENDING -> {
                binding.rvRewards.layoutManager = LinearLayoutManager(this)
                binding.rvRewards.adapter = redemptionAdapter
                binding.btnFooterContainer.visibility = View.GONE
                viewModel.loadPendingRedemptions()
            }
            TAB_COMPLETED -> {
                binding.rvRewards.layoutManager = LinearLayoutManager(this)
                binding.rvRewards.adapter = redemptionAdapter
                binding.btnFooterContainer.visibility = View.GONE
                viewModel.loadCompletedRedemptions()
            }
        }
    }

    private fun onRewardSelected(reward: Reward) {
        selectedReward = reward
        binding.tvButtonText.text = "Confirm Redemption"
        binding.btnFooterContainer.setBackgroundResource(R.drawable.btn_confirm_bg)
        binding.tvButtonText.setTextColor(
            androidx.core.content.ContextCompat.getColor(this, R.color.text_headline)
        )
    }

    private fun resetFooterButton() {
        selectedReward = null
        binding.tvButtonText.text = "Redeem Now"
        binding.btnFooterContainer.setBackgroundResource(R.drawable.btn_redeem_bg)
        binding.tvButtonText.setTextColor(
            androidx.core.content.ContextCompat.getColor(this, R.color.text_inverse)
        )
    }

    private fun showRewardDetails(reward: Reward) {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val dialogBinding = DialogRewardDetailsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvDialogRewardName.text = reward.name
        dialogBinding.tvDialogBrandName.text = reward.brand
        
        // Load logo
        if (reward.logoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(reward.logoUrl)
                .placeholder(R.drawable.mint_coin)
                .error(R.drawable.mint_coin)
                .into(dialogBinding.ivDialogBrandLogo)
        }
        
        dialogBinding.tvRedemptionSteps.text = reward.redemptionSteps
        dialogBinding.tvInstructions.text = reward.instructions
        dialogBinding.tvVerificationTimeline.text = reward.verificationTimeline

        dialogBinding.btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showConfirmationDialog(reward: Reward) {
        val currentBalance = viewModel.balance.value ?: 0L
        
        if (currentBalance < reward.price) {
            AlertDialog.Builder(this)
                .setTitle("Insufficient Balance")
                .setMessage("You need ${reward.price} mints but have $currentBalance.\n\nPlay more quizzes to earn mints!")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Confirm Redemption")
            .setMessage("Are you sure you want to redeem ${reward.name} for ${reward.price} mints?\n\nYour request will be processed by our admin team.")
            .setPositiveButton("Confirm") { _, _ ->
                viewModel.submitRedemption(reward)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

