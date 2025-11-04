package com.health.virtualdoctor.data.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // ⚠️ Remplace par ton URL Cloudflare ou API Gateway
    private const val BASE_URL = " https://macie-unprognosticative-kylan.ngrok-free.dev"

    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    fun getBaseUrl(): String = BASE_URL

    fun init(context: Context) {
        if (retrofit == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = AuthInterceptor(context.applicationContext)

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit!!.create(ApiService::class.java)
        }
    }

    fun getApiService(context: Context): ApiService {
        if (apiService == null) {
            init(context)
        }
        return apiService!!
    }
}
        