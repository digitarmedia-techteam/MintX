package com.digitar.mintx.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.digitar.mintx.databinding.ItemRewardBinding
import com.digitar.mintx.model.Reward

class RewardAdapter(
    private val rewards: List<Reward>,
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

    inner class RewardViewHolder(private val binding: ItemRewardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(reward: Reward, isSelected: Boolean) {
            binding.tvRewardName.text = reward.name
            binding.tvRewardPrice.text = reward.price.toString()
            binding.flBrandContainer.setBackgroundColor(Color.parseColor(reward.colorHex))
            binding.ivBrandLogo.setImageResource(reward.brandLogo)
            
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
