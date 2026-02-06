package com.appslabs.mintx.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.appslabs.mintx.R
import com.appslabs.mintx.databinding.ItemRewardBinding
import com.appslabs.mintx.model.Reward
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

class RewardAdapter(
    private var rewards: List<Reward>,
    private val onRewardClick: (Reward) -> Unit,
    private val onInfoClick: (Reward) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_REWARD = 0
        private const val VIEW_TYPE_NATIVE_AD = 1
    }

    private var selectedPosition = -1
    private var nativeAd: NativeAd? = null

    fun setNativeAd(ad: NativeAd?) {
        nativeAd = ad
        notifyDataSetChanged()
    }

    /**
     * Determines if the position should show an ad
     * Shows ad after every 2 cards, or after 1 card if only 1 exists
     */
    private fun isAdPosition(position: Int): Boolean {
        if (nativeAd == null) return false
        val itemCount = rewards.size
        
        // Special case: if only 1 item, show ad at position 1
        if (itemCount == 1 && position == 1) return true
        
        // Show ad after every 2 items (positions 2, 5, 8, 11, etc.)
        val adPositions = mutableListOf<Int>()
        var pos = 2 // First ad after 2 items
        while (pos <= itemCount + adPositions.size) {
            adPositions.add(pos)
            pos += 3 // 2 items + 1 ad = 3 positions
        }
        
        return position in adPositions
    }

    /**
     * Maps adapter position to actual item position
     */
    private fun getRewardPosition(position: Int): Int {
        var itemPos = position
        // Subtract number of ads before this position
        for (i in 0 until position) {
            if (isAdPosition(i)) itemPos--
        }
        return itemPos
    }

    override fun getItemViewType(position: Int): Int {
        return if (isAdPosition(position)) {
            VIEW_TYPE_NATIVE_AD
        } else {
            VIEW_TYPE_REWARD
        }
    }

    override fun getItemCount(): Int {
        if (nativeAd == null) return rewards.size
        
        val itemCount = rewards.size
        if (itemCount == 0) return 0
        if (itemCount == 1) return 2 // 1 item + 1 ad
        
        // Calculate number of ads to show
        val adCount = (itemCount + 2) / 3 // One ad every 3 positions (2 items + 1 ad)
        return itemCount + adCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_REWARD) {
            val binding = ItemRewardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            RewardViewHolder(binding)
        } else {
            val adView = LayoutInflater.from(parent.context)
                .inflate(R.layout.ad_native_template, parent, false) as NativeAdView
            NativeAdViewHolder(adView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RewardViewHolder) {
            val rewardPosition = getRewardPosition(position)
            if (rewardPosition < rewards.size) {
                holder.bind(rewards[rewardPosition], rewardPosition == selectedPosition)
            }
        } else if (holder is NativeAdViewHolder) {
            nativeAd?.let { holder.bind(it) }
        }
    }
    
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

    class NativeAdViewHolder(private val adView: NativeAdView) :
        RecyclerView.ViewHolder(adView) {

        fun bind(nativeAd: NativeAd) {
            // Set the ad view components
            adView.headlineView = adView.findViewById(R.id.ad_headline)
            adView.bodyView = adView.findViewById(R.id.ad_body)
            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
            adView.iconView = adView.findViewById(R.id.ad_app_icon)
            adView.starRatingView = adView.findViewById(R.id.ad_stars)
            adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
            adView.mediaView = adView.findViewById(R.id.ad_media)

            // Populate the ad view with ad content
            (adView.headlineView as? TextView)?.text = nativeAd.headline
            (adView.bodyView as? TextView)?.text = nativeAd.body
            (adView.callToActionView as? TextView)?.text = nativeAd.callToAction

            // Icon
            if (nativeAd.icon != null) {
                (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
                adView.iconView?.visibility = View.VISIBLE
            } else {
                adView.iconView?.visibility = View.GONE
            }

            // Star rating
            if (nativeAd.starRating != null) {
                (adView.starRatingView as? RatingBar)?.rating = nativeAd.starRating!!.toFloat()
                adView.starRatingView?.visibility = View.VISIBLE
            } else {
                adView.starRatingView?.visibility = View.GONE
            }

            // Advertiser
            if (nativeAd.advertiser != null) {
                (adView.advertiserView as? TextView)?.text = nativeAd.advertiser
                adView.advertiserView?.visibility = View.VISIBLE
            } else {
                adView.advertiserView?.visibility = View.GONE
            }

            // Media view
            if (nativeAd.mediaContent != null) {
                (adView.mediaView as? MediaView)?.mediaContent = nativeAd.mediaContent
                adView.mediaView?.visibility = View.VISIBLE
            } else {
                adView.mediaView?.visibility = View.GONE
            }

            // Register the native ad with the ad view
            adView.setNativeAd(nativeAd)
        }
    }
}

