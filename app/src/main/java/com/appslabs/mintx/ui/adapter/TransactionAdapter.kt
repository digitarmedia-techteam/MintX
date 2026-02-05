package com.appslabs.mintx.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.appslabs.mintx.R
import com.appslabs.mintx.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TRANSACTION = 0
        private const val VIEW_TYPE_AD = 1
        private const val VIEW_TYPE_SHIMMER = 2
        private const val AD_FREQUENCY = 2 // Show ad after every 2 transactions
        private const val PAGE_SIZE = 10 // Load 10 transactions at a time
    }

    private var allTransactions: List<Transaction> = emptyList()
    private var displayedTransactions: MutableList<Transaction> = mutableListOf()
    private var isLoading = false
    private var hasMoreData = true

    fun updateData(newTransactions: List<Transaction>) {
        allTransactions = newTransactions
        displayedTransactions.clear()
        hasMoreData = true
        isLoading = false
        
        // Load first page and notify
        if (allTransactions.isNotEmpty()) {
            val firstPageSize = minOf(PAGE_SIZE, allTransactions.size)
            displayedTransactions.addAll(allTransactions.subList(0, firstPageSize))
            hasMoreData = displayedTransactions.size < allTransactions.size
        }
        
        notifyDataSetChanged()
    }

    fun loadNextPage() {
        if (isLoading || !hasMoreData) return
        
        isLoading = true
        val currentSize = displayedTransactions.size
        val nextPageSize = minOf(PAGE_SIZE, allTransactions.size - currentSize)
        
        if (nextPageSize > 0) {
            val newItems = allTransactions.subList(currentSize, currentSize + nextPageSize)
            displayedTransactions.addAll(newItems)
            notifyItemRangeInserted(getDisplayPosition(currentSize), nextPageSize)
        }
        
        hasMoreData = displayedTransactions.size < allTransactions.size
        isLoading = false
    }

    fun showShimmer() {
        isLoading = true
        notifyDataSetChanged()
    }

    fun hideShimmer() {
        isLoading = false
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if (isLoading && displayedTransactions.isEmpty()) {
            return VIEW_TYPE_SHIMMER
        }
        
        // Calculate actual position in transaction list
        val adCount = position / (AD_FREQUENCY + 1)
        val actualPosition = position - adCount
        
        // Show ad after every AD_FREQUENCY transactions
        return if (position > 0 && (position % (AD_FREQUENCY + 1) == AD_FREQUENCY)) {
            VIEW_TYPE_AD
        } else {
            VIEW_TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_AD -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ad_banner, parent, false)
                AdViewHolder(view)
            }
            VIEW_TYPE_SHIMMER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction_shimmer, parent, false)
                ShimmerViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
                TransactionViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TransactionViewHolder -> {
                val transaction = getTransactionAt(position)
                transaction?.let { holder.bind(it) }
            }
            is AdViewHolder -> {
                holder.loadAd()
            }
            is ShimmerViewHolder -> {
                // Shimmer is handled by layout
            }
        }
    }

    override fun getItemCount(): Int {
        if (isLoading && displayedTransactions.isEmpty()) {
            return 5 // Show 5 shimmer items
        }
        
        // Calculate total items including ads
        val adCount = displayedTransactions.size / AD_FREQUENCY
        return displayedTransactions.size + adCount
    }

    private fun getDisplayPosition(transactionIndex: Int): Int {
        val adCount = transactionIndex / AD_FREQUENCY
        return transactionIndex + adCount
    }

    private fun getTransactionAt(position: Int): Transaction? {
        val adCount = position / (AD_FREQUENCY + 1)
        val actualPosition = position - adCount
        return if (actualPosition < displayedTransactions.size) {
            displayedTransactions[actualPosition]
        } else {
            null
        }
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_tx_icon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_tx_title)
        private val tvDesc: TextView = itemView.findViewById(R.id.tv_tx_desc)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_tx_date)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_tx_amount)

        fun bind(transaction: Transaction) {
            tvTitle.text = transaction.title
            
            if (transaction.description.isNotEmpty()) {
                tvDesc.text = transaction.description
                tvDesc.visibility = View.VISIBLE
            } else {
                tvDesc.visibility = View.GONE
            }
            
            // Format time
            try {
                val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                tvDate.text = dateFormat.format(Date(transaction.getTimestampLong()))
            } catch (e: Exception) {
                tvDate.text = "Unknown Date"
            }

            // Format Amount (Double to Int if whole number)
            val amountVal = transaction.amount
            val amountStr = if (amountVal % 1.0 == 0.0) {
                amountVal.toLong().toString()
            } else {
                String.format("%.2f", amountVal)
            }

            if (transaction.type == "credit") {
                tvAmount.text = "+$amountStr"
                tvAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.mint_green))
                tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.mint_green))
                ivIcon.setColorFilter(null) // Reset tint
            } else {
                tvAmount.text = "-$amountStr"
                tvAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_red))
                tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_red))
            }
        }
    }

    class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val adContainer: FrameLayout = itemView.findViewById(R.id.ad_container)

        fun loadAd() {
            // Keep banner hidden initially
            itemView.visibility = View.GONE
            
            // Load banner ad using AdManager
            (itemView.context as? androidx.fragment.app.FragmentActivity)?.let { activity ->
                com.appslabs.mintx.utils.AdManager.loadBannerAd(activity, adContainer) {
                    // Show banner only when ad is fully loaded
                    itemView.visibility = View.VISIBLE
                }
            }
        }
    }

    class ShimmerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Shimmer effect is handled by the layout itself
    }
}

