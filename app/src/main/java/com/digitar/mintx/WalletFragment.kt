package com.digitar.mintx

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.digitar.mintx.databinding.FragmentWalletBinding
import com.digitar.mintx.ui.adapter.TransactionAdapter
import com.digitar.mintx.ui.wallet.WalletViewModel

class WalletFragment : Fragment() {
    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: WalletViewModel
    private val transactionAdapter = TransactionAdapter()

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
        
        viewModel = ViewModelProvider(this)[WalletViewModel::class.java]
        
        setupRecyclerView()
        observeViewModel()
        
        binding.btnRedeemWallet.setOnClickListener {
             // Handle redeem click
            startActivity(Intent(requireContext(), RewardsStoreActivity::class.java))
//            android.widget.Toast.makeText(context, "Redemption coming soon!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewModel.balance.observe(viewLifecycleOwner) { balance ->
            binding.tvBalance.text = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(balance)
            
            // Approximate value calculation: 200 Coins = 1 INR
            val inrValue = balance / 200.0
            binding.tvApproxValueWallet.text = String.format("≈ ₹%.2f INR", inrValue)
        }

        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.updateData(transactions)
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Could add ProgressBar logic here if UI supported it
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchWalletData() // Refresh on return
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}