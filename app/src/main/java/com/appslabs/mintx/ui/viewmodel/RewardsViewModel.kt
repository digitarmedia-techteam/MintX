package com.appslabs.mintx.ui.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appslabs.mintx.data.repository.RewardsRepository
import com.appslabs.mintx.model.Redemption
import com.appslabs.mintx.model.Reward
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class RewardsViewModel(context: Context) : ViewModel() {
    
    private val repository = RewardsRepository(context)
    private val auth = FirebaseAuth.getInstance()
    
    // Live Data
    private val _rewards = MutableLiveData<List<Reward>>()
    val rewards: LiveData<List<Reward>> = _rewards
    
    private val _balance = MutableLiveData<Long>()
    val balance: LiveData<Long> = _balance
    
    private val _pendingRedemptions = MutableLiveData<List<Redemption>>()
    val pendingRedemptions: LiveData<List<Redemption>> = _pendingRedemptions
    
    private val _completedRedemptions = MutableLiveData<List<Redemption>>()
    val completedRedemptions: LiveData<List<Redemption>> = _completedRedemptions
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _redemptionSuccess = MutableLiveData<Boolean>()
    val redemptionSuccess: LiveData<Boolean> = _redemptionSuccess
    
    /**
     * Load active rewards from Firebase
     */
    fun loadRewards() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            try {
                val rewardsList = repository.getActiveRewards()
                _rewards.value = rewardsList
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load rewards"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Load user's current balance
     */
    fun loadBalance() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userBalance = repository.getUserBalance(userId)
                _balance.value = userBalance
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load balance"
            }
        }
    }
    
    /**
     * Submit redemption request
     */
    fun submitRedemption(reward: Reward) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _redemptionSuccess.value = false
            
            try { val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
                
                // Get user profile
                val (userName, userPhone) = repository.getUserProfile(userId)
                
                // Submit redemption
                val result = repository.submitRedemption(reward, userName, userPhone)
                
                if (result.isSuccess) {
                    _redemptionSuccess.value = true
                    // Reload balance and pending redemptions
                    loadBalance()
                    loadPendingRedemptions()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to submit redemption"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Load pending redemptions
     */
    fun loadPendingRedemptions() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val redemptions = repository.getUserRedemptions(userId, Redemption.STATUS_PENDING)
                _pendingRedemptions.value = redemptions
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load pending redemptions"
            }
        }
    }
    
    /**
     * Load completed redemptions (both approved and rejected)
     */
    fun loadCompletedRedemptions() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                
                // Fetch both approved and rejected redemptions
                val approvedRedemptions = repository.getUserRedemptions(userId, Redemption.STATUS_APPROVED)
                val rejectedRedemptions = repository.getUserRedemptions(userId, Redemption.STATUS_REJECTED)
                
                // Combine and sort by requestedAt (most recent first)
                val allCompleted = (approvedRedemptions + rejectedRedemptions)
                    .sortedByDescending { it.requestedAt?.toDate()?.time ?: 0L }
                
                _completedRedemptions.value = allCompleted
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load completed redemptions"
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}

