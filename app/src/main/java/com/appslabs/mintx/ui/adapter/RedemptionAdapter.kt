package com.appslabs.mintx.ui.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.appslabs.mintx.R
import com.appslabs.mintx.databinding.ItemRedemptionBinding
import com.appslabs.mintx.model.Redemption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class RedemptionAdapter(
    private var redemptions: List<Redemption>
) : RecyclerView.Adapter<RedemptionAdapter.RedemptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RedemptionViewHolder {
        val binding = ItemRedemptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RedemptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RedemptionViewHolder, position: Int) {
        holder.bind(redemptions[position])
    }

    override fun getItemCount() = redemptions.size

    fun updateData(newRedemptions: List<Redemption>) {
        redemptions = newRedemptions
        notifyDataSetChanged()
    }

    inner class RedemptionViewHolder(
        private val binding: ItemRedemptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(redemption: Redemption) {
            binding.apply {
                // Set reward info
                tvRewardName.text = redemption.rewardName
                tvRewardBrand.text = redemption.rewardBrand
                tvPrice.text = String.format("%,d", redemption.rewardPrice)
                
                // Load brand logo
                if (redemption.rewardLogoUrl.isNotEmpty()) {
                    Glide.with(root.context)
                        .load(redemption.rewardLogoUrl)
                        .placeholder(R.drawable.mint_coin)
                        .error(R.drawable.mint_coin)
                        .into(ivBrandLogo)
                }
                
                // Set status with appropriate color
                when (redemption.status) {
                    Redemption.STATUS_PENDING -> {
                        tvStatus.text = "Pending"
                        tvStatus.setBackgroundResource(R.drawable.tab_pill_inactive)
                        tvStatus.setTextColor(ContextCompat.getColor(root.context, R.color.text_headline))
                    }
                    Redemption.STATUS_APPROVED -> {
                        tvStatus.text = "Completed"
                        tvStatus.setBackgroundResource(R.drawable.tab_pill_active)
                        tvStatus.setTextColor(Color.WHITE)
                    }
                    Redemption.STATUS_REJECTED -> {
                        tvStatus.text = "Rejected"
                        tvStatus.setBackgroundColor(ContextCompat.getColor(root.context, R.color.accent_red))
                        tvStatus.setTextColor(Color.WHITE)
                    }
                }
                
                // Set requested date
                redemption.requestedAt?.let { timestamp ->
                    tvRequestedDate.text = getRelativeTime(timestamp.toDate())
                }
                
                // Show redemption code if approved
                if (redemption.status == Redemption.STATUS_APPROVED && !redemption.redemptionCode.isNullOrEmpty()) {
                    layoutRedemptionCode.visibility = View.VISIBLE
                    tvRedemptionCode.text = redemption.redemptionCode
                    
                    btnCopyCode.setOnClickListener {
                        copyToClipboard(root.context, redemption.redemptionCode ?: "")
                    }
                } else {
                    layoutRedemptionCode.visibility = View.GONE
                }
                
                // Show admin notes if rejected
                if (redemption.status == Redemption.STATUS_REJECTED && !redemption.adminNotes.isNullOrEmpty()) {
                    layoutAdminNotes.visibility = View.VISIBLE
                    tvAdminNotes.text = redemption.adminNotes
                } else {
                    layoutAdminNotes.visibility = View.GONE
                }
            }
        }
        
        private fun getRelativeTime(date: Date): String {
            val now = System.currentTimeMillis()
            val diffMillis = now - date.time
            
            return when {
                diffMillis < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diffMillis < TimeUnit.HOURS.toMillis(1) -> {
                    "${TimeUnit.MILLISECONDS.toMinutes(diffMillis)}m ago"
                }
                diffMillis < TimeUnit.DAYS.toMillis(1) -> {
                    "${TimeUnit.MILLISECONDS.toHours(diffMillis)}h ago"
                }
                diffMillis < TimeUnit.DAYS.toMillis(7) -> {
                    "${TimeUnit.MILLISECONDS.toDays(diffMillis)}d ago"
                }
                else -> {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                }
            }
        }
        
        private fun copyToClipboard(context: Context, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Redemption Code", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
        }
    }
}

