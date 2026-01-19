package com.digitar.mintx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.digitar.mintx.databinding.FragmentPredictionBinding

class PredictionFragment : Fragment() {
    private var _binding: FragmentPredictionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPredictionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val options = listOf(binding.btnOptionA, binding.btnOptionDraw, binding.btnOptionB)

        options.forEach { option ->
            option.setOnClickListener {
                options.forEach { it.isSelected = false }
                option.isSelected = true
                binding.btnLockPrediction.isEnabled = true
            }
        }

        binding.btnLockPrediction.setOnClickListener {
            val selectedOption = options.find { it.isSelected }?.text
            Toast.makeText(requireContext(), "Prediction Locked: $selectedOption", Toast.LENGTH_SHORT).show()
            
            // Disable everything once locked
            options.forEach { it.isEnabled = false }
            binding.btnLockPrediction.isEnabled = false
            binding.btnLockPrediction.text = getString(R.string.prediction_locked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}