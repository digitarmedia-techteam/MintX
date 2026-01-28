package com.digitar.mintx.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.digitar.mintx.R
import com.digitar.mintx.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(private var transactions: List<Transaction> = emptyList()) : 
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    fun updateData(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)
    }

    override fun getItemCount() = transactions.size

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
                tvDate.text = dateFormat.format(Date(transaction.timestamp))
            } catch (e: Exception) {
                tvDate.text = "Unknown Date"
            }

            if (transaction.type == "credit") {
                tvAmount.text = "+${transaction.amount}"
                tvAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.mint_green))
                tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.mint_green))
                ivIcon.setColorFilter(null) // Reset tint
            } else {
                tvAmount.text = "-${transaction.amount}"
                tvAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_red))
                // Can apply red tint to icon for debit
                tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_red))
            }
        }
    }
}
