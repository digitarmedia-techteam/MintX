package com.digitar.mintx.auth

import android.app.Activity
import android.app.PendingIntent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.digitar.mintx.R
import com.digitar.mintx.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.HintRequest

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    private val phoneHintLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val credential = result.data?.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
            credential?.id?.let { phoneNumber ->
                // Clean up number (remove +91 etc if needed, but our input handles raw)
                // For this example we just set it.
                // Assuming +91 is default and user picks number.
                // Remove non-digits
                val rawNumber = phoneNumber.replace(Regex("[^0-9]"), "")
                // If it contains country code, strip it if logic dictates. 
                // But simplified: stick last 10 digits
                if (rawNumber.length >= 10) {
                     binding.etMobileNumber.setText(rawNumber.takeLast(10))
                } else {
                     binding.etMobileNumber.setText(rawNumber)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInput()
        setupObservers()
        requestPhoneHint()
    }

    private fun setupInput() {
        binding.ccp.registerCarrierNumberEditText(binding.etMobileNumber)
        
        binding.etMobileNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Check validity using CCP
                binding.btnContinue.isEnabled = binding.ccp.isValidFullNumber
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnContinue.setOnClickListener {
            if (binding.ccp.isValidFullNumber) {
                // Get full number with country code like +919999999999
                val fullNumber = binding.ccp.fullNumberWithPlus
                viewModel.sendOtp(fullNumber, requireActivity())
            } else {
                Toast.makeText(requireContext(), "Enter valid mobile number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginUiState.Loading -> {
                    // Show loader if implemented
                    binding.btnContinue.text = "Processing..."
                    binding.btnContinue.isEnabled = false
                }
                is LoginUiState.OtpSent -> {
                    binding.btnContinue.text = "Continue"
                    binding.btnContinue.isEnabled = true
                    val number = binding.ccp.fullNumberWithPlus // Ensure full E.164
                    val bundle = Bundle().apply {
                        putString("mobileNumber", number)
                    }
                    findNavController().navigate(R.id.action_loginFragment_to_otpFragment, bundle)
                    // Reset state to Idle to avoid re-navigating if we come back
                    // Actually ViewModel is shared? No, generic `by viewModels()` is scoped to Fragment. 
                    // Good, so LoginFragment has its own VM instance separate from Otp if I don't scope to Activity.
                    // But wait, if I want to share data easily I should scope to Activity or pass args.
                    // I am passing args. So separate VMs is fine.
                }
                is LoginUiState.Error -> {
                    binding.btnContinue.text = "Continue"
                    binding.btnContinue.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
    
    // Auto-fill phone number
    private fun requestPhoneHint() {
        val hintRequest = HintRequest.Builder()
            .setPhoneNumberIdentifierSupported(true)
            .build()

        val credentialsClient = Credentials.getClient(requireActivity())
        val intent = credentialsClient.getHintPickerIntent(hintRequest)
        
        try {
            phoneHintLauncher.launch(IntentSenderRequest.Builder(intent.intentSender).build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
