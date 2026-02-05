package com.appslabs.mintx.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.appslabs.mintx.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInput()
        setupObservers()
    }

    private fun setupInput() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateInput()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etFullName.addTextChangedListener(watcher)
        binding.etAge.addTextChangedListener(watcher)

        binding.btnProfileContinue.setOnClickListener {
            val name = binding.etFullName.text.toString()
            val age = binding.etAge.text.toString()
            viewModel.createProfile(name, age)
        }
    }

    private fun validateInput() {
        val name = binding.etFullName.text.toString()
        val age = binding.etAge.text.toString()
        binding.btnProfileContinue.isEnabled = name.isNotBlank() && age.isNotBlank()
    }

    private fun setupObservers() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginUiState.Loading -> {
                     binding.btnProfileContinue.text = "Creating Profile..."
                     binding.btnProfileContinue.isEnabled = false
                }
                is LoginUiState.LoginSuccess -> {
                    // Navigate to Dashboard
                    try {
                        val intent = android.content.Intent(requireContext(), Class.forName("com.appslabs.mintx.MainActivity"))
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Dashboard not found", Toast.LENGTH_SHORT).show()
                    }
                }
                is LoginUiState.Error -> {
                    binding.btnProfileContinue.text = "Finish"
                    binding.btnProfileContinue.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

