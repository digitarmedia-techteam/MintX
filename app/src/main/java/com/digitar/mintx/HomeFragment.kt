package com.digitar.mintx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.digitar.mintx.databinding.FragmentHomeBinding

import com.digitar.mintx.ui.quiz.QuizCategoryBottomSheet

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.cardWallet.setOnClickListener {
            // Navigate to Wallet Details
        }
        
        binding.cardFeatured.setOnClickListener {
            // Navigate to Featured Event (Live Match/Quiz)
        }
        
        binding.btnQuickQuiz.setOnClickListener {
            showQuizCategorySelector()
        }
    }

    private fun showQuizCategorySelector() {
        val bottomSheet = QuizCategoryBottomSheet.newInstance()
        bottomSheet.onQuizStarted = { category ->
            // Logic to start the actual quiz with the selected category
            android.widget.Toast.makeText(requireContext(), "Starting $category Quiz!", android.widget.Toast.LENGTH_SHORT).show()
        }
        bottomSheet.show(childFragmentManager, QuizCategoryBottomSheet.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}