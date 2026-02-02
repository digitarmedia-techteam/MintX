package com.digitar.mintx

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        binding.rvTransactions.apply {
            this.layoutManager = layoutManager
            adapter = transactionAdapter
            setHasFixedSize(false) // Allow dynamic sizing for pagination
            
            // Add scroll listener for pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    // Load more when user scrolls to bottom (last 3 items)
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3
                        && firstVisibleItemPosition >= 0) {
                        transactionAdapter.loadNextPage()
                    }
                }
            })
        }
    }

    private fun observeViewModel() {
        viewModel.balance.observe(viewLifecycleOwner) { balance ->
            binding.tvBalance.text = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(balance)
        }

        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions.isEmpty()) {
                // Show empty state if needed
            } else {
                transactionAdapter.updateData(transactions)
            }
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                transactionAdapter.showShimmer()
            } else {
                transactionAdapter.hideShimmer()
            }
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
    
    companion object {
        private const val COIN_CONVERSION_RATE = 200.0
    }
}
