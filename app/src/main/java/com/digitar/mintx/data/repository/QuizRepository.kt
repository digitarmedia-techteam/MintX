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

class QuizRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("quiz_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun getCategories(): List<QuizCategory> = withContext(Dispatchers.IO) {
        // ... previous cache logic ...
        val cachedJson = sharedPrefs.getString("categories_cache", null)
        if (cachedJson != null) {
            val type = object : TypeToken<List<QuizCategory>>() {}.type
            return@withContext gson.fromJson(cachedJson, type)
        }

        val remoteCategories = ApiConstants.DEFAULT_CATEGORIES.map { QuizCategory(it) }
        sharedPrefs.edit().putString("categories_cache", gson.toJson(remoteCategories)).apply()
        return@withContext remoteCategories
    }

    suspend fun getQuestions(category: String?): List<QuizQuestion>? = withContext(Dispatchers.IO) {
        try {
            val response = com.digitar.mintx.data.network.ApiClient.getQuizApiService().getQuestions(
                apiKey = ApiConstants.API_KEY,
                category = category
            )
            if (response.isSuccessful) {
                return@withContext response.body()
            } else {
                android.util.Log.e("QuizRepository", "API Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("QuizRepository", "Network Exception", e)
        }
        return@withContext null
    }
    
    fun clearCache() {
        sharedPrefs.edit().remove("categories_cache").apply()
    }
}
