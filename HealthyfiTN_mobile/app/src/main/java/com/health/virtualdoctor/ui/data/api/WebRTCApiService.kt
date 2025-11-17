import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface WebRTCApiService {

    @POST("api/webrtc/initiate")
    suspend fun initiateCall(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<CallSessionResponse>

    @POST("api/webrtc/{callId}/offer")
    suspend fun sendOffer(
        @Header("Authorization") token: String,
        @Path("callId") callId: String,
        @Body sdp: Map<String, String>
    ): Response<Map<String, String>>

    @GET("api/webrtc/{callId}")
    suspend fun getCallSession(
        @Header("Authorization") token: String,
        @Path("callId") callId: String
    ): Response<CallSessionResponse>

    @POST("api/webrtc/{callId}/answer")
    suspend fun sendAnswer(
        @Header("Authorization") token: String,
        @Path("callId") callId: String,
        @Body sdp: Map<String, String>
    ): Response<Map<String, String>>

    @POST("api/webrtc/{callId}/ice")
    suspend fun sendIceCandidate(
        @Header("Authorization") token: String,
        @Path("callId") callId: String,
        @Body candidate: Map<String, Any?>
    ): Response<Map<String, String>>

    @POST("api/webrtc/{callId}/end")
    suspend fun endCall(
        @Header("Authorization") token: String,
        @Path("callId") callId: String,
        @Body reason: Map<String, String>
    ): Response<Map<String, String>>
}

data class CallSessionResponse(
    val callId: String,
    val appointmentId: String,
    val doctorId: String,
    val doctorEmail: String,
    val patientId: String,
    val patientEmail: String,
    val callType: String,
    val status: String,
    val initiatorRole: String,
    val iceServers: String?,
    val offerSdp: String?,
    val answerSdp: String?,
    val createdAt: String
)
