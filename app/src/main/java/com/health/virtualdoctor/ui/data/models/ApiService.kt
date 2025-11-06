package com.health.virtualdoctor.ui.data.models

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body refreshToken: String): Response<AuthResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body refreshToken: String): Response<Unit>

    @GET("api/v1/users/profile")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<UserResponse>
    //    // ✅ DOCTOR ACTIVATION SERVICE ENDPOINTS (port 8083)
//    @POST("api/doctors/register")
//    suspend fun registerDoctor(@Body request: DoctorRegisterRequest): Response<DoctorResponse>
//
//    @POST("api/doctors/login")
//    suspend fun loginDoctor(@Body request: Map<String, String>): Response<Map<String, Any>>
// ✅ DOCTOR SERVICE (port 8083)
    @POST("api/doctors/register")
    suspend fun registerDoctor(@Body request: DoctorRegisterRequest): Response<DoctorResponse>

    // ✅ FIX: Le backend attend un LoginRequest, pas Map<String, String>
    @POST("api/doctors/login")
    suspend fun loginDoctor(@Body request: LoginRequest): Response<Map<String, Any>>

}