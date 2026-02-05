package com.appslabs.mintx.auth

import android.app.Activity
import com.appslabs.mintx.data.model.User
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    // Store verification ID temporarily
    var verificationId: String? = null
    var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // Send OTP
    fun sendOtp(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(90L, TimeUnit.SECONDS) // 90s timeout
            .setActivity(activity)
            .setCallbacks(callbacks)
        
        if (resendToken != null) {
            options.setForceResendingToken(resendToken!!)
        }
        
        PhoneAuthProvider.verifyPhoneNumber(options.build())
    }

    // Sign in with Credential
    suspend fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential): Flow<Result<String>> = callbackFlow {
        try {
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        if (user != null) {
                            trySend(Result.success(user.uid))
                        } else {
                            trySend(Result.failure(Exception("User is null")))
                        }
                    } else {
                         trySend(Result.failure(task.exception ?: Exception("Sign in failed")))
                    }
                    close()
                }
        } catch (e: Exception) {
            trySend(Result.failure(e))
            close()
        }
        awaitClose { }
    }
    
    // Firestore: Check if user exists
    suspend fun checkUserExists(uid: String): Boolean {
        return try {
            val docRef = firestore.collection("users").document(uid)
            val snapshot = docRef.get().await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }

    // Firestore: Get User
    suspend fun getUser(uid: String): User? {
        return try {
            val docRef = firestore.collection("users").document(uid)
            val snapshot = docRef.get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Firestore: Create User
    suspend fun createUser(user: User): Result<Boolean> {
        return try {
            val batch = firestore.batch()
            
            // 1. Prepare User Doc with Bonus
            // CRITICAL FIX: Use firebaseUid as the Document ID, not the phone number.
            val userRef = firestore.collection("users").document(user.firebaseUid)
            val bonusUser = user.copy(mintBalance = 100) // Give 100 Mints Bonus
            batch.set(userRef, bonusUser)
            
            // 2. Add Welcome Bonus Transaction
            val transactionRef = userRef.collection("TransactionHistory").document()
            val welcomeTransaction = com.appslabs.mintx.data.model.Transaction(
                id = transactionRef.id,
                title = "Welcome Bonus",
                description = "Login reward for joining MintX",
                amount = 100.0,
                type = "credit",
                status = "completed",
                timestamp = System.currentTimeMillis()
            )
            batch.set(transactionRef, welcomeTransaction)
            
            // Commit Batch
            batch.commit().await()
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getCurrentUser() = auth.currentUser
    
    fun signOut() {
        auth.signOut()
    }
}

