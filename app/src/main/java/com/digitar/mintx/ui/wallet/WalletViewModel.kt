package com.digitar.mintx.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitar.mintx.data.model.Transaction
import com.digitar.mintx.data.repository.WalletRepository
import kotlinx.coroutines.launch

class WalletViewModel : ViewModel() {

    private val repository = WalletRepository()

    private val _balance = MutableLiveData<Long>()
    val balance: LiveData<Long> = _balance

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    init {
        monitorWalletData()
    }

    private fun monitorWalletData() {
        _loading.value = true
        
        viewModelScope.launch {
            repository.getWalletBalanceFlow().collect {
                _balance.value = it
            }
        }
        
        viewModelScope.launch {
            repository.getTransactionsFlow().collect {
                _transactions.value = it
                _loading.value = false
            }
        }
    }

    // Explicit fetch is no longer needed as we use real-time streams
    fun fetchWalletData() {
        // No-op or optional refresh logic
    }
}
