package com.digitar.mintx

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.digitar.mintx.databinding.ActivityRewardsStoreBinding
import com.digitar.mintx.databinding.DialogRewardDetailsBinding
import com.digitar.mintx.model.Reward
import com.digitar.mintx.ui.adapter.RewardAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog

class RewardsStoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardsStoreBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status bar icons should be light for dark background
        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Status bar color handling is done via Theme (transparent)

        setupRewardsList()
        setupClickListeners()
    }

    private fun setupRewardsList() {
        val sampleRewards = listOf(
            Reward("1", "Amazon Pay", "Amazon", 5000, R.drawable.amazon_logo, "#232F3E", verificationTimeline = "Instant"),
            Reward("2", "Flipkart", "Flipkart", 2500, R.drawable.flipcart, "#2874F0", verificationTimeline = "2 - 4 Hours"),
            Reward("4", "Zomato", "Zomato", 3000, R.drawable.zomato_logo, "#CB202D", verificationTimeline = "24 Hours"),
            Reward("5", "Myntra", "Myntra", 1500, R.drawable.myntra, "#FF3F6C", redemptionSteps = "1. Reveal Code\n2. Add to Myntra Credit\n3. Shop anything", verificationTimeline = "Instant"),
            Reward("6", "Nike", "Nike", 8000, R.drawable.nike, "#000000", instructions = "Valid only on official Nike online store and select outlets.", verificationTimeline = "Manual Review (48h)")
        )

        val adapter = RewardAdapter(
            rewards = sampleRewards,
            onRewardClick = { reward -> showConfirmRedemption(reward) },
            onInfoClick = { reward -> showRewardDetails(reward) }
        )

        binding.rvRewards.layoutManager = GridLayoutManager(this, 2)
        binding.rvRewards.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnFooterContainer.setOnClickListener {
            Toast.makeText(this, "Redemption Request Sent!", Toast.LENGTH_SHORT).show()
        }

        binding.btnTabAvailable.setOnClickListener { updateTabs(it) }
        binding.btnTabPending.setOnClickListener { updateTabs(it) }
        binding.btnTabCompleted.setOnClickListener { updateTabs(it) }
    }

    private fun updateTabs(selectedView: View) {
        val tabs = listOf(binding.btnTabAvailable, binding.btnTabPending, binding.btnTabCompleted)
        tabs.forEach { 
            it.setBackgroundResource(if (it == selectedView) R.drawable.tab_pill_active else R.drawable.tab_pill_inactive)
            (it as TextView).setTextColor(if (it == selectedView) 
                androidx.core.content.ContextCompat.getColor(this, R.color.text_inverse) 
            else 
                androidx.core.content.ContextCompat.getColor(this, R.color.text_subtext))
        }
    }

    private fun showRewardDetails(reward: Reward) {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val dialogBinding = DialogRewardDetailsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvDialogRewardName.text = reward.name
        dialogBinding.tvDialogBrandName.text = reward.brand
        dialogBinding.ivDialogBrandLogo.setImageResource(reward.brandLogo)
        dialogBinding.tvRedemptionSteps.text = reward.redemptionSteps
        dialogBinding.tvInstructions.text = reward.instructions
        dialogBinding.tvVerificationTimeline.text = reward.verificationTimeline

        dialogBinding.btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showConfirmRedemption(reward: Reward) {
        binding.tvButtonText.text = "Confirm Redemption"
        binding.btnFooterContainer.setBackgroundResource(R.drawable.btn_confirm_bg)
        binding.tvButtonText.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_headline))
        
        Toast.makeText(this, "Selected: ${reward.name}", Toast.LENGTH_SHORT).show()
    }
}
