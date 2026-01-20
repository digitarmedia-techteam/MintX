package com.digitar.mintx.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.digitar.mintx.data.repository.QuizRepository
import com.digitar.mintx.databinding.BottomSheetQuizCategoriesBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class QuizCategoryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetQuizCategoriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuizViewModel(QuizRepository(requireContext())) as T
            }
        }
    }

    private lateinit var adapter: MainCategoryAdapter
    var onQuizStarted: ((List<String>) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetQuizCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        
        binding.btnStartQuiz.setOnClickListener {
            val selectedSubs = viewModel.categories.value?.flatMap { it.subCategories ?: emptyList() }?.filter { it.isSelected } ?: emptyList()
            if (selectedSubs.isNotEmpty()) {
                onQuizStarted?.invoke(selectedSubs.map { it.name })
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please select at least one sub-category", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        viewModel.fetchCategories()
    }

    private fun setupRecyclerView() {
        adapter = MainCategoryAdapter {
            updateStartButtonState()
        }
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter
    }

    private fun updateStartButtonState() {
        val selectedCount = viewModel.categories.value?.flatMap { it.subCategories ?: emptyList() }?.count { it.isSelected } ?: 0
        binding.btnStartQuiz.isEnabled = selectedCount > 0
        binding.btnStartQuiz.text = if (selectedCount > 0) "Start Quiz ($selectedCount)" else "Select Sub-Topics"
    }

    private fun observeViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            adapter.submitList(categories)
            updateStartButtonState()
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.rvCategories.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "QuizCategoryBottomSheet"
        fun newInstance() = QuizCategoryBottomSheet()
    }
}
