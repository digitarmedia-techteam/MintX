package com.digitar.mintx.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.digitar.mintx.data.model.Task
import com.digitar.mintx.databinding.ItemTaskCardBinding

class TaskAdapter : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.tvTaskTitle.text = task.title
            binding.tvTaskReward.text = task.reward
            binding.tvTaskStatus.text = task.status
            binding.ivTaskIcon.setImageResource(task.iconResId)

            // Step 1 & 2 placeholder text (can be specialized per task if needed)
            // For now, using the data from the model
            
            // Set initial state without animation
            binding.clInstructionDetails.visibility = if (task.isExpanded) View.VISIBLE else View.GONE
            binding.ivInstructionArrow.rotation = if (task.isExpanded) 180f else 0f

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
            
            // Apply animation to the parent view
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup, transition)

            // Update UI
            binding.clInstructionDetails.visibility = if (task.isExpanded) View.VISIBLE else View.GONE
            binding.ivInstructionArrow.animate()
                .rotation(if (task.isExpanded) 180f else 0f)
                .setDuration(300)
                .start()
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem == newItem
    }
}
