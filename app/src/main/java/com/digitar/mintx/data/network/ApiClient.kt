package com.digitar.mintx.data.network

import com.digitar.mintx.data.ApiConstants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private var retrofit: Retrofit? = null

    fun getQuizApiService(): QuizApiService {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(ApiConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(QuizApiService::class.java)
    }
}
