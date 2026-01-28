package com.digitar.mintx.data.repository

import android.content.Context
import com.digitar.mintx.data.ApiConstants
import com.digitar.mintx.data.ManualQuestions
import com.digitar.mintx.data.model.QuizCategory
import com.digitar.mintx.data.model.QuizQuestion
import com.digitar.mintx.data.network.ApiClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class QuizRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("quiz_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun getCategories(): List<QuizCategory> = withContext(Dispatchers.IO) {
        val cachedJson = sharedPrefs.getString("categories_cache_v2", null)
        if (cachedJson != null) {
            val type = object : TypeToken<List<QuizCategory>>() {}.type
            return@withContext gson.fromJson(cachedJson, type)
        }

        val remoteCategories = ApiConstants.QUIZ_CATEGORIES
        sharedPrefs.edit().putString("categories_cache_v2", gson.toJson(remoteCategories)).apply()
        return@withContext remoteCategories
    }

    suspend fun getQuestions(category: String?): List<QuizQuestion>? = withContext(Dispatchers.IO) {
        // PER USER REQUEST: Force usage of ManualQuestions (Bypass API)
        // try {
        //     val response = com.digitar.mintx.data.network.ApiClient.getQuizApiService().getQuestions(
        //         apiKey = ApiConstants.API_KEY,
        //         category = category
        //     )
        //     if (response.isSuccessful) {
        //         val body = response.body()
        //         if (body != null && body.isNotEmpty()) {
        //             return@withContext body
        //         }
        //     }
        //     // Fallback for API error or empty body
        //     android.util.Log.w("QuizRepository", "API failed or empty, using fallback. Code: ${response.code()}")
        //     return@withContext ManualQuestions.getQuestions(category)
        //     
        // } catch (e: Exception) {
        //     android.util.Log.e("QuizRepository", "Network Exception, using fallback", e)
        //     return@withContext ManualQuestions.getQuestions(category)
        // }
        
        // Direct return from ManualQuestions
        // Force NULL to get ALL categories for rotation variety
        return@withContext ManualQuestions.getQuestions(null)
    }
    fun clearCache() {
        sharedPrefs.edit().remove("categories_cache").apply()
    }

    suspend fun getUserBalance(uid: String): Long = withContext(Dispatchers.IO) {
        try {
            val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()
            return@withContext snapshot.getLong("mintBalance") ?: 0L
        } catch (e: Exception) {
            return@withContext 0L
        }
    }

    suspend fun updateUserBalance(uid: String, newBalance: Long) = withContext(Dispatchers.IO) {
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("mintBalance", newBalance)
                .await()
        } catch (e: Exception) {
            // Log or handle error
        }
    }

    suspend fun saveTransaction(uid: String, transaction: com.digitar.mintx.data.model.Transaction) = withContext(Dispatchers.IO) {
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("TransactionHistory")
                .add(transaction)
                .await()
        } catch (e: Exception) {
            android.util.Log.e("QuizRepository", "FIREBASE PERMISSION ERROR: Please checks rules for 'TransactionHistory'.", e)
        }
    }
}
