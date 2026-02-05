package com.appslabs.mintx.data.network

import com.appslabs.mintx.data.model.QuizQuestion
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface QuizApiService {
    @GET("questions")
    suspend fun getQuestions(
        @Query("apiKey") apiKey: String,
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 10
    ): Response<List<QuizQuestion>>
}

