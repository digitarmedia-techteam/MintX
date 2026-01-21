package com.digitar.mintx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.digitar.mintx.databinding.FragmentHomeBinding

import com.digitar.mintx.ui.quiz.QuizCategoryBottomSheet

import android.content.Intent
import java.util.ArrayList

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
//            showQuizCategorySelector()
            navigateToQuiz()
        }

        binding.btnRedeemHome.setOnClickListener {
            startActivity(Intent(requireContext(), RewardsStoreActivity::class.java))
        }

        binding.btnRedeemHome.setOnClickListener {
            startActivity(Intent(requireContext(), RewardsStoreActivity::class.java))
        }

        binding.reward.setOnClickListener {
            startActivity(Intent(requireContext(), RewardsStoreActivity::class.java))
        }

        // Setup Profile Click
        binding.navHeaderAvatar.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }
        
        // Show basic user data
        val sessionManager = com.digitar.mintx.utils.SessionManager(requireContext())
        val name = sessionManager.getUserName()
        if (name != null) {
            binding.tvGreeting.text = "Hello, $name 👋"
        }
    }

    private fun showQuizCategorySelector() {
        val bottomSheet = QuizCategoryBottomSheet.newInstance()
        bottomSheet.onQuizStarted = { categories ->
            // Pass categories via Fragment Result
            parentFragmentManager.setFragmentResult("quiz_request", Bundle().apply {
                putStringArrayList("categories", ArrayList(categories))
            })
            
            // Navigate to Quiz tab
            navigateToQuiz()
        }
        bottomSheet.show(childFragmentManager, QuizCategoryBottomSheet.TAG)
    }

    private fun navigateToQuiz() {
        val mainActivity = requireActivity() as MainActivity
        mainActivity
            .findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.bottom_navigation
            )
            .selectedItemId = R.id.navigation_quiz
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}