package com.appslabs.mintx.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.appslabs.mintx.R
import com.appslabs.mintx.databinding.ItemRewardBinding
import com.appslabs.mintx.model.Reward

class RewardAdapter(
    private var rewards: List<Reward>,
    private val onRewardClick: (Reward) -> Unit,
    private val onInfoClick: (Reward) -> Unit
) : RecyclerView.Adapter<RewardAdapter.RewardViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val binding = ItemRewardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RewardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        holder.bind(rewards[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = rewards.size
    
    fun updateData(newRewards: List<Reward>) {
        rewards = newRewards
        selectedPosition = -1 // Reset selection
        notifyDataSetChanged()
    }

    inner class RewardViewHolder(private val binding: ItemRewardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(reward: Reward, isSelected: Boolean) {
            binding.tvRewardName.text = reward.name
            binding.tvRewardPrice.text = String.format("%,d", reward.price)
            binding.flBrandContainer.setBackgroundColor(Color.parseColor(reward.colorHex))
            
            // Load logo from URL using Glide
            if (reward.logoUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(reward.logoUrl)
                    .placeholder(R.drawable.mint_coin)
                    .error(R.drawable.mint_coin)
                    .into(binding.ivBrandLogo)
            } else {
                binding.ivBrandLogo.setImageResource(R.drawable.mint_coin)
            }
            
            binding.ivRewardInfo.setOnClickListener {
                onInfoClick(reward)
            }

            binding.ivRewardInfo.setOnLongClickListener {
                onInfoClick(reward)
                true
            }

            // Highlight selection
            binding.cardReward.strokeWidth = if (isSelected) 6 else 0
            
            binding.root.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onRewardClick(reward)
            }
        }
    }
}

