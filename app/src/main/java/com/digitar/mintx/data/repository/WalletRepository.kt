package com.digitar.mintx.data.repository

import com.digitar.mintx.data.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WalletRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Keep One-Shot fetch for legacy/simple usage
    suspend fun getWalletBalance(): Long = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext 0L
        try {
            val snapshot = db.collection("users").document(uid).get().await()
            return@withContext snapshot.getLong("mintBalance") ?: 0L
        } catch (e: Exception) {
            return@withContext 0L
        }
    }

    // Real-Time Balance Stream
    fun getWalletBalanceFlow(): Flow<Long> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }
        
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // close(error) // Don't close stream on transient error, maybe log
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val balance = snapshot.getLong("mintBalance") ?: 0L
                    trySend(balance)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getTransactions(): List<Transaction> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            val snapshot = db.collection("users")
                .document(uid)
                .collection("TransactionHistory")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()

            val transactions = snapshot.toObjects(Transaction::class.java)
            return@withContext transactions
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    // Real-Time Transaction Stream
    fun getTransactionsFlow(): Flow<List<Transaction>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }

        val listener = db.collection("users")
            .document(uid)
            .collection("TransactionHistory")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val transactions = snapshot.toObjects(Transaction::class.java)
                    trySend(transactions)
                }
            }
        awaitClose { listener.remove() }
    }
}
