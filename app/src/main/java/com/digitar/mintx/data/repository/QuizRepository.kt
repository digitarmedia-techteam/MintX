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

    suspend fun getUserCategories(uid: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()
            
            @Suppress("UNCHECKED_CAST")
            return@withContext (snapshot.get("categories") as? List<String>) ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("QuizRepo", "Error fetching user categories", e)
            return@withContext emptyList()
        }
    }

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
            val allQuestions = mutableListOf<QuizQuestion>()

            if (!categories.isNullOrEmpty()) {
                // Fetch questions for EACH category separately to ensure a mix
                // Firestore 'whereIn' + 'limit' often returns results from just one category if data is clustered
                for (category in categories.take(10)) {
                    val snapshot = db.collection("questions")
                        .whereEqualTo("category", category)
                        .limit(5) // Fetch 5 questions per category
                        .get()
                        .await()

                    val batch = snapshot.toObjects(QuizQuestion::class.java)
                    snapshot.documents.forEachIndexed { index, doc ->
                        if (index < batch.size) batch[index].id = doc.id
                    }
                    allQuestions.addAll(batch)
                }
            } else {
                 // Fallback if no categories provided
                 val snapshot = db.collection("questions").limit(20).get().await()
                 val batch = snapshot.toObjects(QuizQuestion::class.java)
                 snapshot.documents.forEachIndexed { index, doc ->
                    if (index < batch.size) batch[index].id = doc.id
                 }
                 allQuestions.addAll(batch)
            }
            
            // Failsafe: If specific fetch returned nothing (e.g. typos in category names), fetch generic
            if (allQuestions.isEmpty() && !categories.isNullOrEmpty()) {
                 android.util.Log.w("QuizRepo", "No questions found for specific categories. Fetching general fallback.")
                 val snapshot = db.collection("questions").limit(20).get().await()
                 val batch = snapshot.toObjects(QuizQuestion::class.java)
                 snapshot.documents.forEachIndexed { index, doc ->
                    if (index < batch.size) batch[index].id = doc.id
                 }
                 allQuestions.addAll(batch)
            }
            
            return@withContext allQuestions.shuffled()
            
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
