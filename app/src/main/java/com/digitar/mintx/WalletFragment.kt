package com.digitar.mintx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.digitar.mintx.databinding.FragmentWalletBinding

class WalletFragment : Fragment() {
    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initializing with sample data for UI demonstration
        binding.tvBalance.text = "2,450"

        // Dummy Transaction 2
        binding.tx2.apply {
            tvTxTitle.text = "Cricket Prediction Win"
            tvTxAmount.text = "+120"
            tvTxDate.text = "Jan 18, 08:45 PM"
        }

        // Dummy Transaction 3
        binding.tx3.apply {
            tvTxTitle.text = "Amazon Task Completed"
            tvTxAmount.text = "+150"
            tvTxDate.text = "Jan 17, 11:20 AM"
            tvTxAmount.setTextColor(android.graphics.Color.parseColor("#00E676"))
        }

        // Dummy Transaction 4
        binding.tx4.apply {
            tvTxTitle.text = "Reward Redeemed"
            tvTxAmount.text = "-500"
            tvTxDate.text = "Jan 15, 10:00 AM"
            tvTxAmount.setTextColor(android.graphics.Color.parseColor("#FF4B2B"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}