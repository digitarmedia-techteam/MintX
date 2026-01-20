package com.digitar.mintx.ui.quiz

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.digitar.mintx.R
import com.digitar.mintx.data.model.QuizCategory
import com.digitar.mintx.data.model.SubCategory
import com.digitar.mintx.databinding.ItemCategoryMainBinding
import com.digitar.mintx.databinding.ItemSubcategoryBinding

class MainCategoryAdapter(
    private val onSubCategorySelected: () -> Unit
) : ListAdapter<QuizCategory, MainCategoryAdapter.MainViewHolder>(MainDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = ItemCategoryMainBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MainViewHolder(private val binding: ItemCategoryMainBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: QuizCategory) {
            binding.tvCatName.text = category.name
            binding.tvCatDesc.text = category.description
            
            val subs = category.subCategories ?: emptyList()
            
            val subAdapter = SubCategoryAdapter { 
                onSubCategorySelected()
            }
            binding.rvSubCategories.layoutManager = GridLayoutManager(binding.root.context, 2)
            binding.rvSubCategories.adapter = subAdapter
            subAdapter.submitList(subs)

            // Update expanded state
            binding.rvSubCategories.visibility = if (category.isExpanded) View.VISIBLE else View.GONE
            binding.ivExpand.rotation = if (category.isExpanded) 180f else 0f
            
            // Highlight if any subcategory is selected
            val hasSelection = subs.any { it.isSelected }
            binding.cardMainCategory.strokeColor = if (hasSelection) 
                ContextCompat.getColor(binding.root.context, R.color.mint_gold) 
            else 
                ContextCompat.getColor(binding.root.context, R.color.accent_glass_border)

            binding.clMainRow.setOnClickListener {
                category.isExpanded = !category.isExpanded
                notifyItemChanged(adapterPosition)
            }
        }
    }

    private class MainDiffCallback : DiffUtil.ItemCallback<QuizCategory>() {
        override fun areItemsTheSame(oldItem: QuizCategory, newItem: QuizCategory): Boolean =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: QuizCategory, newItem: QuizCategory): Boolean =
            oldItem == newItem
    }
}

class SubCategoryAdapter(
    private val onSelected: (SubCategory) -> Unit
) : ListAdapter<SubCategory, SubCategoryAdapter.SubViewHolder>(SubDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubViewHolder {
        val binding = ItemSubcategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubViewHolder(private val binding: ItemSubcategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(sub: SubCategory) {
            binding.tvSubName.text = sub.name
            
            if (sub.isSelected) {
                binding.cardSubCategory.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.mint_surface_light))
                binding.cardSubCategory.strokeColor = ContextCompat.getColor(binding.root.context, R.color.mint_gold)
                binding.ivSubCheck.visibility = View.VISIBLE
                binding.tvSubName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.mint_gold))
            } else {
                binding.cardSubCategory.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.mint_surface))
                binding.cardSubCategory.strokeColor = ContextCompat.getColor(binding.root.context, R.color.accent_glass_border)
                binding.ivSubCheck.visibility = View.GONE
                binding.tvSubName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_headline))
            }

            binding.root.setOnClickListener {
                sub.isSelected = !sub.isSelected
                notifyItemChanged(adapterPosition)
                onSelected(sub)
            }
        }
    }

    private class SubDiffCallback : DiffUtil.ItemCallback<SubCategory>() {
        override fun areItemsTheSame(oldItem: SubCategory, newItem: SubCategory): Boolean =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SubCategory, newItem: SubCategory): Boolean =
            oldItem == newItem
    }
}
