package com.example.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApiService {
    @POST("v1/chat/completions")
    suspend fun generateContent(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String,
        @Header("X-Title") title: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://openrouter.ai/api/"

    private val okHttpClient by lazy {
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = if (com.example.BuildConfig.DEBUG) okhttp3.logging.HttpLoggingInterceptor.Level.BODY else okhttp3.logging.HttpLoggingInterceptor.Level.NONE
        }
        okhttp3.OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private val moshi by lazy {
        com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
    }

    val service: OpenRouterApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenRouterApiService::class.java)
    }
}
