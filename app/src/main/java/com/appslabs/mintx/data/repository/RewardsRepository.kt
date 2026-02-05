package com.appslabs.mintx.data.repository

import android.content.Context
import android.util.Log
import com.appslabs.mintx.model.Redemption
import com.appslabs.mintx.model.Reward
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RewardsRepository(private val context: Context) {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Fetch all active rewards from Firestore
     */
    suspend fun getActiveRewards(): List<Reward> = withContext(Dispatchers.IO) {
        try {
            // Try with index first
            try {
                val snapshot = db.collection("rewards")
                    .whereEqualTo("isActive", true)
                    .orderBy("price", Query.Direction.ASCENDING)
                    .get()
                    .await()
                
                val rewards = snapshot.toObjects(Reward::class.java)
                snapshot.documents.forEachIndexed { index, doc ->
                    if (index < rewards.size) rewards[index].id = doc.id
                }
                
                Log.d("RewardsRepo", "Fetched ${rewards.size} active rewards (with index)")
                return@withContext rewards
                
            } catch (indexError: Exception) {
                // Fallback: Fetch all and filter/sort client-side
                Log.w("RewardsRepo", "Index error, using client-side filter: ${indexError.message}")
                
                val allSnapshot = db.collection("rewards").get().await()
                val allRewards = allSnapshot.toObjects(Reward::class.java)
                allSnapshot.documents.forEachIndexed { index, doc ->
                    if (index < allRewards.size) allRewards[index].id = doc.id
                }
                
                // Filter active and sort by price
                val activeRewards = allRewards
                    .filter { it.isActive }
                    .sortedBy { it.price }
                
                Log.d("RewardsRepo", "Fetched ${activeRewards.size} active rewards (client-side filter)")
                return@withContext activeRewards
            }
            
        } catch (e: Exception) {
            Log.e("RewardsRepo", "Error fetching rewards", e)
            emptyList()
        }
    }
    
    /**
     * Get user's current balance
     */
    suspend fun getUserBalance(userId: String): Long = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("users")
                .document(userId)
                .get()
                .await()
            
            // Check both 'balance' and 'mintBalance' for compatibility
            val balance = snapshot.getLong("balance") ?: snapshot.getLong("mintBalance") ?: 0L
            Log.d("RewardsRepo", "User balance: $balance")
            balance
        } catch (e: Exception) {
            Log.e("RewardsRepo", "Error fetching balance", e)
            0L
        }
    }
    
    /**
     * Get user profile data (name, phone)
     */
    suspend fun getUserProfile(userId: String): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("users")
                .document(userId)
                .get()
                .await()
            
            val name = snapshot.getString("name") ?: "Unknown User"
            val phone = snapshot.getString("phone") ?: "N/A"
            
            Pair(name, phone)
        } catch (e: Exception) {
            Log.e("RewardsRepo", "Error fetching user profile", e)
            Pair("Unknown User", "N/A")
        }
    }
    
    /**
     * Submit a redemption request
     */
    suspend fun submitRedemption(
        reward: Reward,
        userName: String,
        userPhone: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )
            
            // Check balance
            val balance = getUserBalance(userId)
            if (balance < reward.price) {
                return@withContext Result.failure(
                    Exception("Insufficient balance. You need ${reward.price} mints but have $balance.")
                )
            }
            
            // Create redemption request
            val redemption = Redemption(
                userId = userId,
                userName = userName,
                userPhone = userPhone,
                rewardId = reward.id,
                rewardName = reward.name,
                rewardBrand = reward.brand,
                rewardPrice = reward.price,
                rewardLogoUrl = reward.logoUrl,
                status = Redemption.STATUS_PENDING,
                requestedAt = Timestamp.now()
            )
            
            val docRef = db.collection("redemptions")
                .add(redemption)
                .await()
            
            Log.d("RewardsRepo", "Redemption submitted: ${docRef.id}")
            Result.success(docRef.id)
            
        } catch (e: Exception) {
            Log.e("RewardsRepo", "Error submitting redemption", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get user's redemptions filtered by status
     */
    suspend fun getUserRedemptions(
        userId: String,
        status: String? = null
    ): List<Redemption> = withContext(Dispatchers.IO) {
        try {
            // Try with index first
            try {
                var query = db.collection("redemptions")
                    .whereEqualTo("userId", userId)
                
                if (status != null) {
                    query = query.whereEqualTo("status", status)
                }
                
                val snapshot = query
                    .orderBy("requestedAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val redemptions = snapshot.toObjects(Redemption::class.java)
                snapshot.documents.forEachIndexed { index, doc ->
                    if (index < redemptions.size) redemptions[index].id = doc.id
                }
                
                Log.d("RewardsRepo", "Fetched ${redemptions.size} redemptions (with index) for user $userId (status: $status)")
                return@withContext redemptions
                
            } catch (indexError: Exception) {
                // Fallback: Fetch user's redemptions and filter/sort client-side
                Log.w("RewardsRepo", "Index error, using client-side filter: ${indexError.message}")
                
                val allSnapshot = db.collection("redemptions")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                
                var redemptions = allSnapshot.toObjects(Redemption::class.java)
                allSnapshot.documents.forEachIndexed { index, doc ->
                    if (index < redemptions.size) redemptions[index].id = doc.id
                }
                
                // Filter by status if specified
                if (status != null) {
                    redemptions = redemptions.filter { it.status == status }
                }
                
                // Sort by requestedAt descending
                redemptions = redemptions.sortedByDescending { 
                    it.requestedAt?.toDate()?.time ?: 0L 
                }
                
                Log.d("RewardsRepo", "Fetched ${redemptions.size} redemptions (client-side) for user $userId (status: $status)")
                return@withContext redemptions
            }
            
        } catch (e: Exception) {
            Log.e("RewardsRepo", "Error fetching redemptions", e)
            emptyList()
        }
    }
}

