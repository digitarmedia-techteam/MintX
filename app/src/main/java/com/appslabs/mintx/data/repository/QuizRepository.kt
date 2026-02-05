package com.appslabs.mintx.data.repository

import android.content.Context
import com.appslabs.mintx.data.ApiConstants

import com.appslabs.mintx.data.model.QuizCategory
import com.appslabs.mintx.data.model.QuizQuestion
import com.appslabs.mintx.data.network.ApiClient
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

    suspend fun getQuestions(categories: List<String>?, questionCount: Int = 35): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val questionTracker = com.appslabs.mintx.utils.QuestionTracker(context)
        
        try {
            val allQuestions = mutableListOf<QuizQuestion>()

            if (!categories.isNullOrEmpty()) {
                // Fetch questions for EACH category separately to ensure a mix
                // Firestore 'whereIn' + 'limit' often returns results from just one category if data is clustered
                for (category in categories.take(10)) {
                    val snapshot = db.collection("questions")
                        .whereEqualTo("category", category)
                        .limit(50) // Fetch more initially to account for filtering
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
                 val snapshot = db.collection("questions").limit(100).get().await()
                 val batch = snapshot.toObjects(QuizQuestion::class.java)
                 snapshot.documents.forEachIndexed { index, doc ->
                    if (index < batch.size) batch[index].id = doc.id
                 }
                 allQuestions.addAll(batch)
            }
            
            // Failsafe: If specific fetch returned nothing (e.g. typos in category names), fetch generic
            if (allQuestions.isEmpty() && !categories.isNullOrEmpty()) {
                 android.util.Log.w("QuizRepo", "No questions found for specific categories. Fetching general fallback.")
                 val snapshot = db.collection("questions").limit(100).get().await()
                 val batch = snapshot.toObjects(QuizQuestion::class.java)
                 snapshot.documents.forEachIndexed { index, doc ->
                    if (index < batch.size) batch[index].id = doc.id
                 }
                 allQuestions.addAll(batch)
            }
            
            // Filter questions based on answer history
            val availableQuestions = allQuestions.filter { question ->
                questionTracker.isQuestionAvailable(question.id)
            }
            
            // Group questions by difficulty
            val easyQuestions = availableQuestions.filter { it.difficulty.equals("Easy", ignoreCase = true) }.shuffled()
            val mediumQuestions = availableQuestions.filter { it.difficulty.equals("Medium", ignoreCase = true) }.shuffled()
            val hardQuestions = availableQuestions.filter { it.difficulty.equals("Hard", ignoreCase = true) }.shuffled()
            
            android.util.Log.d("QuizRepo", "Total fetched: ${allQuestions.size}, Available after filtering: ${availableQuestions.size}")
            android.util.Log.d("QuizRepo", "Easy: ${easyQuestions.size}, Medium: ${mediumQuestions.size}, Hard: ${hardQuestions.size}")
            android.util.Log.d("QuizRepo", "Tracker Stats:\n${questionTracker.getStats()}")
            
            // Calculate distribution: 60% Easy, 20% Medium, 20% Hard
            val easyCount = (questionCount * 0.6).toInt()
            val mediumCount = (questionCount * 0.2).toInt()
            val hardCount = questionCount - easyCount - mediumCount // Remainder to ensure total equals questionCount
            
            android.util.Log.d("QuizRepo", "Distribution for $questionCount questions: Easy=$easyCount, Medium=$mediumCount, Hard=$hardCount")
            
            // Build the final list with proper distribution
            val finalQuestions = mutableListOf<QuizQuestion>()
            finalQuestions.addAll(easyQuestions.take(easyCount))
            finalQuestions.addAll(mediumQuestions.take(mediumCount))
            finalQuestions.addAll(hardQuestions.take(hardCount))
            
            // If we don't have enough questions of a specific difficulty, fill from others
            if (finalQuestions.size < questionCount) {
                val needed = questionCount - finalQuestions.size
                val remaining = (easyQuestions + mediumQuestions + hardQuestions).filter { it !in finalQuestions }
                finalQuestions.addAll(remaining.take(needed))
                android.util.Log.w("QuizRepo", "Not enough questions for perfect distribution. Filled ${needed} from remaining pool.")
            }
            
            // Shuffle the final list so difficulties are mixed
            return@withContext finalQuestions.shuffled()
            
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

    suspend fun getUserXP(uid: String): Long = withContext(Dispatchers.IO) {
        try {
            val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()
            return@withContext snapshot.getLong("totalXP") ?: 0L
        } catch (e: Exception) {
            return@withContext 0L
        }
    }

    suspend fun updateUserXP(uid: String, newXP: Long) = withContext(Dispatchers.IO) {
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("totalXP", newXP)
                .await()
        } catch (e: Exception) {
            // Log or handle error
        }
    }

    suspend fun saveTransaction(uid: String, transaction: com.appslabs.mintx.data.model.Transaction) = withContext(Dispatchers.IO) {
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

    suspend fun updateUserSolvedStats(uid: String, easyDelta: Int, mediumDelta: Int, hardDelta: Int) = withContext(Dispatchers.IO) {
        if (easyDelta == 0 && mediumDelta == 0 && hardDelta == 0) return@withContext
        
        try {
            val updates = HashMap<String, Any>()
            if (easyDelta > 0) updates["solvedEasy"] = com.google.firebase.firestore.FieldValue.increment(easyDelta.toLong())
            if (mediumDelta > 0) updates["solvedMedium"] = com.google.firebase.firestore.FieldValue.increment(mediumDelta.toLong())
            if (hardDelta > 0) updates["solvedHard"] = com.google.firebase.firestore.FieldValue.increment(hardDelta.toLong())
            
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update(updates)
                .await()
        } catch (e: Exception) {
            android.util.Log.e("QuizRepository", "Error updating solved stats", e)
        }
    }
}

