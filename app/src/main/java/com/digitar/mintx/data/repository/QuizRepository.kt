package com.digitar.mintx.data.repository

import android.content.Context
import com.digitar.mintx.data.ApiConstants

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
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        try {
             // Collection name corrected to match Firestore screenshot: 'quiz_categories'
             val result = db.collection("quiz_categories").get().await()
             
             if (result.isEmpty) {
                 android.util.Log.w("QuizRepo", "No categories found in 'quiz_categories'")
             }
             
             val categories = result.documents.map { doc ->
                 QuizCategory(
                     id = doc.id,
                     name = doc.getString("name") ?: "Unknown",
                     description = doc.getString("description")?.takeIf { it != "nan" } ?: "",
                     icon = null, // TODO: Add dynamic icon support (URL)
                     subCategories = emptyList() 
                 )
             }
             return@withContext categories
        } catch (e: Exception) {
            android.util.Log.e("QuizRepo", "Error fetching categories", e)
             return@withContext emptyList()
        }
    }

    suspend fun getQuestions(categories: List<String>?): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        try {
            var query: com.google.firebase.firestore.Query = db.collection("questions")
            
            // Handle multiple categories
            if (!categories.isNullOrEmpty()) {
                 // Convert to lowercase for matching if stored that way, or keep original. 
                 // Admin panel saves them as is (often capitalized 'Linux', 'DevOps').
                 query = query.whereIn("category", categories.take(10)) 
            }
            
            // Randomize approach:
            // Since we can't easily random sort in Firestore, we fetch a larger batch and shuffle locally.
            var snapshot = query.limit(50).get().await()
            
            // FALLBACK: If specific category has no questions (e.g. data mismatch), fetch ANY questions
            if (snapshot.isEmpty && !categories.isNullOrEmpty()) {
                android.util.Log.w("QuizRepo", "No questions found for categories $categories. Fetching fallback.")
                snapshot = db.collection("questions").limit(50).get().await()
            }
            val questions = snapshot.toObjects(QuizQuestion::class.java)
            
            // Ensure IDs are set
            snapshot.documents.forEachIndexed { index, doc ->
                 if (index < questions.size) questions[index].id = doc.id
            }
            
            return@withContext questions.shuffled()
            
        } catch (e: Exception) {
            android.util.Log.e("QuizRepo", "Error fetching questions from DB", e)
            return@withContext emptyList()
        }
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
