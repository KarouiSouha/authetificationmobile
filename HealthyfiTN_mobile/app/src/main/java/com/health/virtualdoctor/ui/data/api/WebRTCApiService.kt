package com.health.virtualdoctor.ui.data.api

import retrofit2.Response
import retrofit2.http.*

interface WebRTCApiService {

    @POST("api/webrtc/initiate")
    suspend fun initiateCall(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<CallSessionResponse>

    @POST("api/webrtc/calls/{callId}/offer")
    suspend fun sendOffer(
        @Header("Authorization") token: String,
        @Path("callId") callId: String,
        @Body sdp: Map<String, String>
    ): Response<Void>

    @POST("api/webrtc/calls/{callId}/answer")
    suspend fun sendAnswer(
        @Header("Authorization") token: String,
        @Path("callId") callId: String,
        @Body sdp: Map<String, String>
    ): Response<Void>

    @POST("api/webrtc/calls/{callId}/ice-candidate")
    suspend fun sendIceCandidate(
        @Header("Authorization") token: String,
        @Path("callId") callId: String,
        @Body candidate: Map<String, Any>
    ): Response<Void>

    @GET("api/webrtc/calls/{callId}")
    suspend fun getCallSession(
        @Header("Authorization") token: String,
        @Path("callId") callId: String
    ): Response<CallSessionResponse>

    @POST("api/webrtc/calls/{callId}/end")
    suspend fun endCall(
        @Header("Authorization") token: String,
        @Path("callId") callId: String,
        @Body reason: Map<String, String>
    ): Response<Void>
}

// Data classes
data class CallSessionResponse(
    val callId: String,
    val appointmentId: String,
    val callType: String,
    val status: String,
    val iceServers: String?,
    val offerSdp: String?,
    val answerSdp: String?
)