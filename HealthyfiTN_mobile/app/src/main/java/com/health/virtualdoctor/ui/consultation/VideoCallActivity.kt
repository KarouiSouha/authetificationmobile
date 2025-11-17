package com.health.virtualdoctor.ui.consultation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.health.virtualdoctor.R
import com.health.virtualdoctor.ui.data.api.RetrofitClient
import com.health.virtualdoctor.ui.utils.TokenManager
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.webrtc.*

/**
 * VideoCallActivity - Gestion des appels vidéo/audio avec WebRTC
 *
 * Flow:
 * 1. Récupérer ICE servers du backend
 * 2. Initialiser WebRTC (PeerConnectionFactory)
 * 3. Créer PeerConnection
 * 4. Ajouter local MediaStream (caméra + micro)
 * 5. Créer Offer SDP (si initiateur) ou attendre Offer
 * 6. Envoyer SDP au backend (signaling)
 * 7. Recevoir Answer SDP
 * 8. Échanger ICE candidates
 * 9. Connexion établie → afficher vidéo distante
 * 10. Terminer appel → nettoyer ressources
 */
class VideoCallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VideoCallActivity"
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val AUDIO_PERMISSION_REQUEST = 101
    }

    // UI Elements
    private lateinit var localSurfaceView: SurfaceViewRenderer
    private lateinit var remoteSurfaceView: SurfaceViewRenderer
    private lateinit var tvCallStatus: TextView
    private lateinit var tvCallDuration: TextView
    private lateinit var btnToggleMute: ImageButton
    private lateinit var btnToggleVideo: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnEndCall: MaterialButton

    // WebRTC Components
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var videoCapturer: VideoCapturer? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var eglBase: EglBase? = null

    // Signaling
    private lateinit var tokenManager: TokenManager
    private var callId: String? = null
    private var appointmentId: String? = null
    private var isInitiator = false
    private var callType = "VIDEO" // VIDEO ou AUDIO

    // State
    private var isMuted = false
    private var isVideoEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        tokenManager = TokenManager(this)

        // Récupérer données de l'intent
        appointmentId = intent.getStringExtra("appointmentId")
        callType = intent.getStringExtra("callType") ?: "VIDEO"
        isInitiator = intent.getBooleanExtra("isInitiator", false)

        if (appointmentId == null) {
            Toast.makeText(this, "❌ Appointment ID manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        checkPermissions()
    }

    private fun initViews() {
        localSurfaceView = findViewById(R.id.localSurfaceView)
        remoteSurfaceView = findViewById(R.id.remoteSurfaceView)
        tvCallStatus = findViewById(R.id.tvCallStatus)
        tvCallDuration = findViewById(R.id.tvCallDuration)
        btnToggleMute = findViewById(R.id.btnToggleMute)
        btnToggleVideo = findViewById(R.id.btnToggleVideo)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnEndCall = findViewById(R.id.btnEndCall)

        // Listeners
        btnToggleMute.setOnClickListener { toggleMute() }
        btnToggleVideo.setOnClickListener { toggleVideo() }
        btnSwitchCamera.setOnClickListener { switchCamera() }
        btnEndCall.setOnClickListener { endCall() }

        // Masquer vidéo locale si appel audio
        if (callType == "AUDIO") {
            localSurfaceView.visibility = View.GONE
            remoteSurfaceView.visibility = View.GONE
            btnToggleVideo.visibility = View.GONE
            btnSwitchCamera.visibility = View.GONE
        }
    }

    private fun checkPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED && callType == "VIDEO"
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                CAMERA_PERMISSION_REQUEST
            )
        } else {
            initializeWebRTC()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeWebRTC()
            } else {
                Toast.makeText(this, "❌ Permissions refusées", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // ============================================================
    // WEBRTC INITIALIZATION
    // ============================================================

    private fun initializeWebRTC() {
        lifecycleScope.launch {
            try {
                tvCallStatus.text = "🔄 Initialisation..."

                // 1. Initier l'appel (créer CallSession dans le backend)
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val request = mapOf(
                    "appointmentId" to appointmentId!!,
                    "callType" to callType
                )

                val response = RetrofitClient.getWebRTCService(this@VideoCallActivity)
                    .initiateCall(token, request)

                if (response.isSuccessful && response.body() != null) {
                    val callSession = response.body()!!
                    callId = callSession.callId

                    Log.d(TAG, "✅ Call session created: $callId")
                    Log.d(TAG, "ICE Servers: ${callSession.iceServers}")

                    // 2. Initialiser WebRTC Factory
                    setupWebRTCFactory()

                    // 3. Créer PeerConnection avec ICE servers
                    setupPeerConnection(callSession.iceServers)

                    // 4. Démarrer capture locale (caméra + micro)
                    startLocalMedia()

                    // 5. Si initiateur, créer offer SDP
                    if (isInitiator) {
                        createOffer()
                    } else {
                        // Sinon, attendre l'offer (polling ou WebSocket)
                        waitForOffer()
                    }

                    tvCallStatus.text =
                        if (isInitiator) "📞 Appel en cours..." else "📞 Réception de l'appel..."

                } else {
                    Log.e(TAG, "❌ Failed to initiate call: ${response.code()}")
                    Toast.makeText(this@VideoCallActivity, "❌ Erreur d'appel", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error initializing WebRTC: ${e.message}", e)
                Toast.makeText(this@VideoCallActivity, "❌ Erreur: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    private fun setupWebRTCFactory() {
        // Initialize EGL context
        eglBase = EglBase.create()

        // Initialize PeerConnectionFactory
        val options = PeerConnectionFactory.InitializationOptions.builder(applicationContext)
            .setEnableInternalTracer(true)
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase!!.eglBaseContext,
            true,
            true
        )

        val decoderFactory = DefaultVideoDecoderFactory(eglBase!!.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()

        Log.d(TAG, "✅ PeerConnectionFactory initialized")
    }

    private fun setupPeerConnection(iceServersJson: String?) {
        // Parse ICE servers from backend (Metered.ca)
        val iceServers = mutableListOf<PeerConnection.IceServer>()

        try {
            if (!iceServersJson.isNullOrEmpty()) {
                // Parse JSON array
                val jsonArray = JSONArray(iceServersJson)

                for (i in 0 until jsonArray.length()) {
                    val server = jsonArray.getJSONObject(i)
                    val urls = server.getString("urls")

                    if (server.has("username") && server.has("credential")) {
                        // TURN server avec auth
                        iceServers.add(
                            PeerConnection.IceServer.builder(urls)
                                .setUsername(server.getString("username"))
                                .setPassword(server.getString("credential"))
                                .createIceServer()
                        )
                        Log.d(TAG, "✅ TURN server added: $urls")
                    } else {
                        // STUN server (pas d'auth)
                        iceServers.add(
                            PeerConnection.IceServer.builder(urls)
                                .createIceServer()
                        )
                        Log.d(TAG, "✅ STUN server added: $urls")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing ICE servers: ${e.message}")
        }

        // Fallback si parsing échoue
        if (iceServers.isEmpty()) {
            Log.w(TAG, "⚠️ Using fallback STUN server")
            iceServers.add(
                PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80")
                    .createIceServer()
            )
        }

        Log.d(TAG, "🧊 Total ICE servers configured: ${iceServers.size}")

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType = PeerConnection.IceTransportsType.ALL
        }

        peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        Log.d(TAG, "🧊 New ICE candidate: ${it.sdp}")
                        sendIceCandidate(it)
                    }
                }

                override fun onDataChannel(dataChannel: DataChannel?) {
                    Log.d(TAG, "📡 Data channel: ${dataChannel?.label()}")
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    Log.d(TAG, "❄️ ICE Connection State: $state")
                    runOnUiThread {
                        when (state) {
                            PeerConnection.IceConnectionState.CONNECTED -> {
                                tvCallStatus.text = "✅ Connecté"
                            }
                            PeerConnection.IceConnectionState.DISCONNECTED -> {
                                tvCallStatus.text = "⚠️ Déconnecté"
                            }
                            PeerConnection.IceConnectionState.FAILED -> {
                                tvCallStatus.text = "❌ Échec de connexion"
                                Toast.makeText(this@VideoCallActivity, "Connexion échouée", Toast.LENGTH_SHORT).show()
                            }
                            PeerConnection.IceConnectionState.CHECKING -> {
                                tvCallStatus.text = "🔄 Connexion en cours..."
                            }
                            else -> {}
                        }
                    }
                }

                override fun onAddStream(stream: MediaStream?) {
                    Log.d(TAG, "📹 Remote stream added")
                    runOnUiThread {
                        stream?.videoTracks?.firstOrNull()?.addSink(remoteSurfaceView)
                    }
                }

                // ✅ CORRIGÉ: onSignalingChange au lieu de onSignalingStateChange
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                    Log.d(TAG, "📡 Signaling State: $state")
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                    Log.d(TAG, "🧊 ICE candidates removed")
                }

                override fun onRemoveStream(stream: MediaStream?) {
                    Log.d(TAG, "📹 Remote stream removed")
                }

                override fun onRenegotiationNeeded() {
                    Log.d(TAG, "🔄 Renegotiation needed")
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {
                    Log.d(TAG, "❄️ ICE receiving: $receiving")
                }

                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                    Log.d(TAG, "❄️ ICE Gathering State: $state")
                }
            }
        )

        Log.d(TAG, "✅ PeerConnection created with Metered.ca TURN servers")
    }
    private fun startLocalMedia() {
        // Initialize SurfaceViewRenderer
        if (callType == "VIDEO") {
            localSurfaceView.init(eglBase!!.eglBaseContext, null)
            localSurfaceView.setMirror(true)
            localSurfaceView.setEnableHardwareScaler(true)

            remoteSurfaceView.init(eglBase!!.eglBaseContext, null)
            remoteSurfaceView.setEnableHardwareScaler(true)
        }

        // Create video source
        if (callType == "VIDEO") {
            val videoSource = peerConnectionFactory.createVideoSource(false)
            videoCapturer = createVideoCapturer()

            videoCapturer?.initialize(
                SurfaceTextureHelper.create("CaptureThread", eglBase!!.eglBaseContext),
                applicationContext,
                videoSource.capturerObserver
            )

            videoCapturer?.startCapture(1280, 720, 30)

            localVideoTrack = peerConnectionFactory.createVideoTrack("local_video", videoSource)
            localVideoTrack?.addSink(localSurfaceView)
        }

        // Create audio source
        val audioConstraints = MediaConstraints()
        val audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("local_audio", audioSource)

        // Create local stream and add tracks
        val localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        if (callType == "VIDEO") {
            localStream.addTrack(localVideoTrack)
        }
        localStream.addTrack(localAudioTrack)

        peerConnection?.addStream(localStream)

        Log.d(TAG, "✅ Local media started")
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(this)
        val deviceNames = enumerator.deviceNames

        // Try front camera first
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }

        // Fallback to back camera
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }

        return null
    }

    // ============================================================
    // SIGNALING (SDP/ICE Exchange)
    // ============================================================

    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo",
                    if (callType == "VIDEO") "true" else "false"
                )
            )
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(SimpleSdpObserver(), it)
                    sendOfferSdp(it.description)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "❌ Create offer failed: $error")
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "❌ Set local description failed: $error")
            }
        }, constraints)
    }

    private fun sendOfferSdp(sdp: String) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val body = mapOf("sdp" to sdp)

                val response = RetrofitClient.getWebRTCService(this@VideoCallActivity)
                    .sendOffer(token, callId!!, body)

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Offer SDP sent")
                    // Wait for answer
                    waitForAnswer()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending offer: ${e.message}")
            }
        }
    }

    private fun waitForAnswer() {
        // TODO: Implémenter polling ou WebSocket pour recevoir answer SDP
        // Pour simplifier, utilisez polling toutes les 2 secondes

        lifecycleScope.launch {
            repeat(30) { // Max 60 secondes
                kotlinx.coroutines.delay(2000)

                try {
                    val token = "Bearer ${tokenManager.getAccessToken()}"
                    val response = RetrofitClient.getWebRTCService(this@VideoCallActivity)
                        .getCallSession(token, callId!!)

                    if (response.isSuccessful && response.body() != null) {
                        val session = response.body()!!
                        if (!session.answerSdp.isNullOrEmpty()) {
                            Log.d(TAG, "✅ Answer SDP received")
                            receiveAnswerSdp(session.answerSdp)
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error polling answer: ${e.message}")
                }
            }
        }
    }

    private fun receiveAnswerSdp(sdp: String) {
        val answerSdp = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection?.setRemoteDescription(SimpleSdpObserver(), answerSdp)
        Log.d(TAG, "✅ Remote description set (Answer)")
    }

    private fun waitForOffer() {
        // Le patient attend l'offre SDP du docteur
        lifecycleScope.launch {
            repeat(30) { // Max 60 secondes d'attente
                kotlinx.coroutines.delay(2000)

                try {
                    val token = "Bearer ${tokenManager.getAccessToken()}"
                    val response = RetrofitClient.getWebRTCService(this@VideoCallActivity)
                        .getCallSession(token, callId!!)

                    if (response.isSuccessful && response.body() != null) {
                        val session = response.body()!!

                        if (!session.offerSdp.isNullOrEmpty()) {
                            Log.d(TAG, "✅ Offer SDP received")
                            receiveOfferSdp(session.offerSdp)
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error polling offer: ${e.message}")
                }
            }

            // Timeout
            runOnUiThread {
                Toast.makeText(
                    this@VideoCallActivity,
                    "⏰ L'appelant n'a pas répondu",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun receiveOfferSdp(sdp: String) {
        val offerSdp = SessionDescription(SessionDescription.Type.OFFER, sdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "✅ Remote description set (Offer)")
                // Créer answer
                createAnswer()
            }

            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "❌ Set remote description failed: $error")
            }
        }, offerSdp)
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo",
                    if (callType == "VIDEO") "true" else "false"
                )
            )
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(SimpleSdpObserver(), it)
                    sendAnswerSdp(it.description)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "❌ Create answer failed: $error")
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "❌ Set local description failed: $error")
            }
        }, constraints)
    }

    private fun sendAnswerSdp(sdp: String) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val body = mapOf("sdp" to sdp)

                val response = RetrofitClient.getWebRTCService(this@VideoCallActivity)
                    .sendAnswer(token, callId!!, body)

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Answer SDP sent")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending answer: ${e.message}")
            }
        }
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val body = mapOf(
                    "candidate" to candidate.sdp,
                    "sdpMid" to candidate.sdpMid,
                    "sdpMLineIndex" to candidate.sdpMLineIndex
                )

                RetrofitClient.getWebRTCService(this@VideoCallActivity)
                    .sendIceCandidate(token, callId!!, body)

                Log.d(TAG, "✅ ICE candidate sent")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending ICE: ${e.message}")
            }
        }
    }

    // ============================================================
    // CONTROLS
    // ============================================================

    private fun toggleMute() {
        isMuted = !isMuted
        localAudioTrack?.setEnabled(!isMuted)

        runOnUiThread {
            btnToggleMute.setImageResource(
                if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic_on
            )
            Toast.makeText(
                this,
                if (isMuted) "🔇 Micro coupé" else "🎤 Micro activé",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun toggleVideo() {
        isVideoEnabled = !isVideoEnabled
        localVideoTrack?.setEnabled(isVideoEnabled)

        runOnUiThread {
            localSurfaceView.visibility = if (isVideoEnabled) View.VISIBLE else View.GONE
            btnToggleVideo.setImageResource(
                if (isVideoEnabled) R.drawable.ic_videocam_on else R.drawable.ic_videocam_off
            )
            Toast.makeText(
                this,
                if (isVideoEnabled) "📹 Caméra activée" else "📹 Caméra coupée",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun switchCamera() {
        videoCapturer?.let {
            if (it is CameraVideoCapturer) {
                it.switchCamera(null)
                Toast.makeText(this, "🔄 Caméra changée", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun endCall() {
        lifecycleScope.launch {
            try {
                if (callId != null) {
                    val token = "Bearer ${tokenManager.getAccessToken()}"
                    val body = mapOf("reason" to "COMPLETED")

                    RetrofitClient.getWebRTCService(this@VideoCallActivity)
                        .endCall(token, callId!!, body)

                    Log.d(TAG, "✅ Call ended")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error ending call: ${e.message}")
            } finally {
                cleanup()
                finish()
            }
        }
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    private fun cleanup() {
        try {
            localVideoTrack?.removeSink(localSurfaceView)
            localVideoTrack?.dispose()
            localAudioTrack?.dispose()

            videoCapturer?.stopCapture()
            videoCapturer?.dispose()

            peerConnection?.close()
            peerConnection?.dispose()

            localSurfaceView.release()
            remoteSurfaceView.release()

            eglBase?.release()

            Log.d(TAG, "✅ Resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cleanup error: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    // ============================================================
    // SIMPLE SDP OBSERVER (Helper Class)
    // ============================================================

    private class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}