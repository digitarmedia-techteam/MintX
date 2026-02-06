package com.appslabs.mintx.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.appslabs.mintx.data.model.Task
import com.appslabs.mintx.databinding.ItemTaskCardBinding
import com.appslabs.mintx.R
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

class TaskAdapter : ListAdapter<Task, RecyclerView.ViewHolder>(TaskDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_TASK = 0
        private const val VIEW_TYPE_NATIVE_AD = 1
    }

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
        val itemCount = currentList.size
        
        // Special case: if only 1 item, show ad at position 1
        if (itemCount == 1 && position == 1) return true
        
        // Show ad after every 2 items (positions 2, 5, 8, 11, etc.)
        // But only if we haven't run out of items
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
    private fun getItemPosition(position: Int): Int {
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
            VIEW_TYPE_TASK
        }
    }

    override fun getItemCount(): Int {
        if (nativeAd == null) return currentList.size
        
        val itemCount = currentList.size
        if (itemCount == 0) return 0
        if (itemCount == 1) return 2 // 1 item + 1 ad
        
        // Calculate number of ads to show
        val adCount = (itemCount + 2) / 3 // One ad every 3 positions (2 items + 1 ad)
        return itemCount + adCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_TASK) {
            val binding = ItemTaskCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            TaskViewHolder(binding)
        } else {
            val adView = LayoutInflater.from(parent.context)
                .inflate(R.layout.ad_native_template, parent, false) as NativeAdView
            NativeAdViewHolder(adView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TaskViewHolder) {
            val itemPosition = getItemPosition(position)
            if (itemPosition < currentList.size) {
                holder.bind(currentList[itemPosition])
            }
        } else if (holder is NativeAdViewHolder) {
            nativeAd?.let { holder.bind(it) }
        }
    }

    inner class TaskViewHolder(private val binding: ItemTaskCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.tvTaskTitle.text = task.title
            binding.tvTaskReward.text = task.reward
            binding.tvTaskStatus.text = task.status
            binding.ivTaskIcon.setImageResource(task.iconResId)

            // Set initial state without animation
            binding.clInstructionDetails.visibility = if (task.isExpanded) View.VISIBLE else View.GONE
            binding.ivInstructionArrow.rotation = if (task.isExpanded) 180f else 0f

            // Card click listener triggers expansion
            binding.root.setOnClickListener {
                toggleExpansion(task)
            }
            
            binding.llInstructionHeader.setOnClickListener {
                toggleExpansion(task)
            }
        }

        private fun toggleExpansion(task: Task) {
            task.isExpanded = !task.isExpanded

            // Create smooth transition
            val transition = AutoTransition().apply {
                duration = 300
            }
            
            // Apply animation to the RecyclerView (parent) to push other items
            (binding.root.parent as? ViewGroup)?.let { recyclerView ->
                TransitionManager.beginDelayedTransition(recyclerView, transition)
            }

            // Update Visibility
            binding.clInstructionDetails.visibility = if (task.isExpanded) View.VISIBLE else View.GONE
            
            // Animate Arrow Rotation
            binding.ivInstructionArrow.animate()
                .rotation(if (task.isExpanded) 180f else 0f)
                .setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
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

    private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem == newItem
    }
}

