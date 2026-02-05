package com.appslabs.mintx.auth

import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
// import androidx.navigation.fragment.navArgs
import com.appslabs.mintx.R
import com.appslabs.mintx.databinding.FragmentOtpBinding
import com.google.android.gms.auth.api.phone.SmsRetriever

class OtpFragment : Fragment(), SmsBroadcastReceiver.OtpReceiveListener {

    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels() // Shared VM
    private var mobileNumber: String = ""

    private val smsBroadcastReceiver by lazy { SmsBroadcastReceiver() }
    
    // OTP Box list
    private lateinit var otpBoxes: List<com.appslabs.mintx.utils.OtpEditText>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mobileNumber = arguments?.getString("mobileNumber") ?: ""
        
        smsBroadcastReceiver.setOtpListener(this)
        startSmsListener()

        setupOtpInputs()
        setupObservers()
        
        binding.tvPhoneNumber.text = mobileNumber
        
        binding.btnResend.setOnClickListener {
            viewModel.resendOtp(mobileNumber, requireActivity())
            startSmsListener()
        }
        
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        binding.tvGoBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun startSmsListener() {
        val client = SmsRetriever.getClient(requireActivity())
        client.startSmsRetriever()
    }

    private fun setupOtpInputs() {
        otpBoxes = listOf(
            binding.otp1 as com.appslabs.mintx.utils.OtpEditText,
            binding.otp2 as com.appslabs.mintx.utils.OtpEditText,
            binding.otp3 as com.appslabs.mintx.utils.OtpEditText,
            binding.otp4 as com.appslabs.mintx.utils.OtpEditText,
            binding.otp5 as com.appslabs.mintx.utils.OtpEditText,
            binding.otp6 as com.appslabs.mintx.utils.OtpEditText
        )

        for (i in otpBoxes.indices) {
            // Forward Navigation
            otpBoxes[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) {
                        if (i < otpBoxes.size - 1) {
                            otpBoxes[i + 1].requestFocus()
                        } else {
                            verifyOtp()
                        }
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            // Backward Navigation (Backspace on empty)
            otpBoxes[i].setOnBackspacePressedListener {
                if (i > 0) {
                    otpBoxes[i - 1].requestFocus()
                    // Optional: Move cursor to end if needed, but for 1 char box it's fine.
                    // If we want to CLEAR the previous box as well (super aggressive delete):
                    // otpBoxes[i - 1].text?.clear()
                    // But standard behavior is just navigate back so user can decide.
                }
            }
        }
    }

    private fun verifyOtp() {
        val otp = otpBoxes.joinToString("") { it.text.toString() }
        if (otp.length == 6) {
            viewModel.verifyOtp(otp)
        }
    }

    private fun setupObservers() {
        viewModel.timerText.observe(viewLifecycleOwner) { 
            // format: "Resend SMS in XXs" or "Resend SMS"
            if (it == "Resend OTP") {
                binding.tvResendTimer.text = "Resend SMS"
                binding.tvResendTimer.setTextColor(resources.getColor(R.color.mint_primary, null))
                binding.tvResendTimer.setOnClickListener { 
                    viewModel.resendOtp(mobileNumber, requireActivity())
                    startSmsListener()
                }
            } else {
                binding.tvResendTimer.text = "Resend SMS in $it"
                binding.tvResendTimer.setOnClickListener(null)
            }
        }
        
        viewModel.timerSeconds.observe(viewLifecycleOwner) { seconds ->
             if (binding.tvResendTimer.text.toString() == "Resend SMS") return@observe
             
             val colorStart = resources.getColor(R.color.mint_green, null)
             val colorMiddle = resources.getColor(R.color.mint_gold, null)
             val colorEnd = resources.getColor(R.color.accent_red, null)
             
             val color = when {
                 seconds > 60 -> colorStart
                 seconds > 30 -> colorMiddle
                 else -> colorEnd
             }
             binding.tvResendTimer.setTextColor(color)
        }

        viewModel.isResendEnabled.observe(viewLifecycleOwner) { enabled ->
             // Handled by text click above
        }

        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    otpBoxes.forEach { it.isEnabled = false }
                }
                is LoginUiState.LoginSuccess -> {
                    // Navigate to Dashboard
                    binding.progressBar.visibility = View.GONE
                    // Use standard Intent to start Dashboard Activity
                    // Assuming MainActivity is dashboard or specifically requested Dashboard
                    // User: "Navigate directly to Dashboard Activity"
                    // I will start MainActivity and clear task
                    try {
                        val intent = android.content.Intent(requireContext(), Class.forName("com.appslabs.mintx.MainActivity"))
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Dashboard not found", Toast.LENGTH_SHORT).show()
                    }
                }
                is LoginUiState.NavigateToProfile -> {
                    binding.progressBar.visibility = View.GONE
                    otpBoxes.forEach { it.isEnabled = true } // Re-enable? Or Navigate away.
                    // Navigate to Profile
                     findNavController().navigate(R.id.action_otpFragment_to_profileFragment)
                }
                is LoginUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    otpBoxes.forEach { it.isEnabled = true }
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    otpBoxes.forEach { it.isEnabled = true }
                }
            }
        }
    }

    override fun onOtpReceived(otp: String) {
        // Auto-fill
        if (otp.length == 6) {
            for (i in otp.indices) {
                otpBoxes[i].setText(otp[i].toString())
            }
            // Logic in textwatcher will trigger verify
        }
    }

    override fun onOtpTimeout() {
        Toast.makeText(requireContext(), "SMS retrieval timed out", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(smsBroadcastReceiver, intentFilter, SmsRetriever.SEND_PERMISSION, null, android.content.Context.RECEIVER_EXPORTED)
        } else {
            requireActivity().registerReceiver(smsBroadcastReceiver, intentFilter, SmsRetriever.SEND_PERMISSION, null)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            requireActivity().unregisterReceiver(smsBroadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

