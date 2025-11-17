package com.health.virtualdoctor.ui.data.api

import WebRTCApiService
import android.content.Context
import com.health.virtualdoctor.ui.data.models.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import java.util.concurrent.TimeUnit
import retrofit2.Response
import retrofit2.http.*
object RetrofitClient {

    // ✅ URLs des services via Cloudflare Tunnels
    private const val AUTH_BASE_URL =
        "https://guru-border-consist-network.trycloudflare.com" // Port 8082
    private const val DOCTOR_BASE_URL =
        "https://cheaper-disclose-hospital-salvation.trycloudflare.com" // Port 8083
    private const val NOTIFICATION_BASE_URL =
        "https://normal-maintaining-antenna-his.trycloudflare.com" // Port 8084
    private const val USER_BASE_URL =
        "https://substance-fda-innovation-enable.trycloudflare.com" // Port 8085


    // Votre configuration existante...
    private const val DOCTOR_SERVICE_BASE_URL = "http://10.0.2.2:8083/"

    // ✅ AJOUTEZ CETTE FONCTION
    fun getWebRTCService(context: Context): WebRTCApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(DOCTOR_SERVICE_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(WebRTCApiService::class.java)
    }

    private var authRetrofit: Retrofit? = null
    private var doctorRetrofit: Retrofit? = null
    private var notificationRetrofit: Retrofit? = null
    private var userRetrofit: Retrofit? = null
    private var videoCallRetrofit: Retrofit? = null

    private var authApiService: ApiService? = null
    private var doctorApiService: ApiService? = null
    private var notificationApiService: ApiService? = null
    private var userApiService: ApiService? = null

    private var appContext: Context? = null

    // ✅ Init function for compatibility
    fun init(context: Context) {
        appContext = context.applicationContext
        getAuthService(appContext!!)
    }

    // ✅ AUTH Service (port 8082)
    fun getAuthService(context: Context): ApiService {
        if (authApiService == null) {
            authRetrofit = createRetrofit(AUTH_BASE_URL, context, true)
            authApiService = authRetrofit!!.create(ApiService::class.java)
        }
        return authApiService!!
    }

    // ✅ DOCTOR Service (port 8083)
    fun getDoctorService(context: Context): ApiService {
        if (doctorApiService == null) {
            doctorRetrofit = createRetrofit(DOCTOR_BASE_URL, context, true)
            doctorApiService = doctorRetrofit!!.create(ApiService::class.java)
        }
        return doctorApiService!!
    }

    // ✅ NOTIFICATION Service (port 8084)
    fun getNotificationService(context: Context): ApiService {
        if (notificationApiService == null) {
            notificationRetrofit = createRetrofit(NOTIFICATION_BASE_URL, context, true)
            notificationApiService = notificationRetrofit!!.create(ApiService::class.java)
        }
        return notificationApiService!!
    }

    // ✅ USER Service (port 8085)
    fun getUserService(context: Context): ApiService {
        if (userApiService == null) {
            userRetrofit = createRetrofit(USER_BASE_URL, context, true)
            userApiService = userRetrofit!!.create(ApiService::class.java)
        }
        return userApiService!!
    }


    // ✅ Default service (for backward compatibility)
    @Deprecated("Use getAuthService(), getDoctorService(), getUserService(), or getNotificationService() instead")
    fun getApiService(context: Context): ApiService {
        return getAuthService(context)
    }

    /**
     * Create Retrofit instance with proper configuration
     * @param baseUrl Base URL for the service
     * @param context Application context
     * @param includeAuthInterceptor Whether to include auth interceptor
     */
    private fun createRetrofit(
        baseUrl: String,
        context: Context,
        includeAuthInterceptor: Boolean = true
    ): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Add auth interceptor only if needed
        if (includeAuthInterceptor) {
            val authInterceptor = AuthInterceptor(context.applicationContext)
            clientBuilder.addInterceptor(authInterceptor)
        }

        // Add header for Cloudflare/ngrok tunnels
        clientBuilder.addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("ngrok-skip-browser-warning", "true")
                .build()
            chain.proceed(request)
        }

        val okHttpClient = clientBuilder.build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ✅ Getters pour les URLs (utiles pour debug)
    fun getAuthBaseUrl(): String = AUTH_BASE_URL
    fun getDoctorBaseUrl(): String = DOCTOR_BASE_URL
    fun getNotificationBaseUrl(): String = NOTIFICATION_BASE_URL
    fun getUserBaseUrl(): String = USER_BASE_URL


    /**
     * Clear all cached instances (useful for logout)
     */
    fun clearAll() {
        authApiService = null
        doctorApiService = null
        notificationApiService = null
        userApiService = null

        authRetrofit = null
        doctorRetrofit = null
        notificationRetrofit = null
        userRetrofit = null
        videoCallRetrofit = null
    }
}