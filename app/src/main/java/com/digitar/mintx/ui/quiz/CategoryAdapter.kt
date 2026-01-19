package com.digitar.mintx.ui.quiz

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.digitar.mintx.R
import com.digitar.mintx.data.model.QuizCategory
import com.digitar.mintx.databinding.ItemCategoryBinding

class CategoryAdapter(private val onCategorySelected: (QuizCategory) -> Unit) :
    ListAdapter<QuizCategory, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: QuizCategory) {
            binding.tvCategoryName.text = category.name
            
            // Highlight selected state
            if (category.isSelected) {
                binding.cardCategory.strokeColor = ContextCompat.getColor(binding.root.context, R.color.mint_gold)
                binding.cardCategory.strokeWidth = 4
                binding.ivCategoryIcon.setTint(ContextCompat.getColor(binding.root.context, R.color.mint_gold))
            } else {
                binding.cardCategory.strokeColor = ContextCompat.getColor(binding.root.context, R.color.accent_glass_border)
                binding.cardCategory.strokeWidth = 1
                binding.ivCategoryIcon.setTint(ContextCompat.getColor(binding.root.context, R.color.text_subtext))
            }

            binding.root.setOnClickListener {
                onCategorySelected(category)
            }
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<QuizCategory>() {
        override fun areItemsTheSame(oldItem: QuizCategory, newItem: QuizCategory): Boolean =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: QuizCategory, newItem: QuizCategory): Boolean =
            oldItem == newItem
    }
}

// Extension function for ImageView tint
fun android.widget.ImageView.setTint(color: Int) {
    this.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
}
