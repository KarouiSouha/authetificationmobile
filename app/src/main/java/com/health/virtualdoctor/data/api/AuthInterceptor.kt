package com.health.virtualdoctor.data.api

import android.content.Context
import com.health.virtualdoctor.utils.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.OkHttpClient

class AuthInterceptor(private val context: Context) : Interceptor {

    private val tokenManager = TokenManager(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Ne pas ajouter de token pour les endpoints d'authentification
        if (originalRequest.url.encodedPath.contains("/auth/login") ||
            originalRequest.url.encodedPath.contains("/auth/register")) {
            return chain.proceed(originalRequest)
        }

        // Ajouter le token d’accès s’il existe
        val accessToken = tokenManager.getAccessToken()
        val requestWithToken = if (accessToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        // Exécuter la requête
        var response = chain.proceed(requestWithToken)

        // Si 401 => essayer de rafraîchir le token
        if (response.code == 401 && accessToken != null) {
            response.close()

            synchronized(this) {
                val newAccessToken = refreshToken()

                if (newAccessToken != null) {
                    // Refaire la requête avec le nouveau token
                    val newRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()

                    response = chain.proceed(newRequest)
                } else {
                    // Échec du refresh → déconnexion
                    tokenManager.clearTokens()
                    // TODO: Rediriger vers l’écran de login si nécessaire
                }
            }
        }

        return response
    }

    private fun refreshToken(): String? = runBlocking {
        try {
            val refreshToken = tokenManager.getRefreshToken() ?: return@runBlocking null

            // Créer un client sans interceptor pour éviter la boucle infinie
            val client = OkHttpClient.Builder().build()

            // Construire le corps JSON
            val jsonBody = JSONObject().apply {
                put("refreshToken", refreshToken)
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = RequestBody.create(mediaType, jsonBody)

            val request = Request.Builder()
                .url("${RetrofitClient.getBaseUrl()}api/v1/auth/refresh")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(response.body?.string() ?: "")
                val newAccessToken = jsonResponse.getString("accessToken")
                val newRefreshToken = jsonResponse.getString("refreshToken")

                tokenManager.saveTokens(newAccessToken, newRefreshToken)
                return@runBlocking newAccessToken
            }

            return@runBlocking null

        } catch (e: Exception) {
            e.printStackTrace()
            return@runBlocking null
        }
    }
}
