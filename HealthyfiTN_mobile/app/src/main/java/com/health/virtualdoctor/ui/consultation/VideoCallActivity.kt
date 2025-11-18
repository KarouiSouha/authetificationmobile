package com.health.virtualdoctor.ui.consultation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.webrtc.*

class VideoCallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VideoCallActivity"
        private const val PERMISSIONS_REQUEST = 100
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
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
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var eglBase: EglBase? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null

    // Signaling
    private lateinit var tokenManager: TokenManager
    private var callId: String? = null
    private var appointmentId: String? = null
    private var isInitiator = true
    private var callType = "VIDEO"

    // State
    private var isMuted = false
    private var isVideoEnabled = true
    private var isDestroyed = false
    private var isPermissionsGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Keep screen on during call
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // ✅ Set immersive mode for better experience
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }

        setContentView(R.layout.activity_video_call)

        tokenManager = TokenManager(this)

        // Get appointment ID
        appointmentId = intent.getStringExtra("appointmentId")
        callType = intent.getStringExtra("callType") ?: "VIDEO"
        isInitiator = intent.getBooleanExtra("isInitiator", true)

        if (appointmentId == null) {
            Toast.makeText(this, "❌ Appointment ID manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "📞 VideoCall starting for appointment: $appointmentId")

        initViews()
        checkAndRequestPermissions()
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

        // Set initial states
        btnToggleMute.setImageResource(R.drawable.ic_mic_on)
        btnToggleVideo.setImageResource(R.drawable.ic_videocam_on)

        btnToggleMute.setOnClickListener { toggleMute() }
        btnToggleVideo.setOnClickListener { toggleVideo() }
        btnSwitchCamera.setOnClickListener { switchCamera() }
        btnEndCall.setOnClickListener { endCall() }

        if (callType == "AUDIO") {
            localSurfaceView.visibility = View.GONE
            remoteSurfaceView.visibility = View.GONE
            btnToggleVideo.visibility = View.GONE
            btnSwitchCamera.visibility = View.GONE
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            Log.d(TAG, "🔐 Requesting permissions: ${permissionsNeeded.joinToString()}")
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                PERMISSIONS_REQUEST
            )
        } else {
            Log.d(TAG, "✅ All permissions granted")
            isPermissionsGranted = true
            initializeWebRTC()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                Log.d(TAG, "✅ All permissions granted")
                isPermissionsGranted = true
                initializeWebRTC()
            } else {
                Log.e(TAG, "❌ Permissions denied")
                Toast.makeText(this, "❌ Permissions refusées - L'appel ne peut pas fonctionner", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun initializeWebRTC() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                if (isDestroyed || !isPermissionsGranted) return@launch

                tvCallStatus.text = "🔄 Initialisation..."
                Log.d(TAG, "🔄 Starting WebRTC initialization")

                // Initialize EGL first
                eglBase = EglBase.create()

                // Initialize surface views on UI thread
                localSurfaceView.init(eglBase?.eglBaseContext, null)
                localSurfaceView.setMirror(true)
                localSurfaceView.setEnableHardwareScaler(true)
                localSurfaceView.setZOrderMediaOverlay(true)

                remoteSurfaceView.init(eglBase?.eglBaseContext, null)
                remoteSurfaceView.setEnableHardwareScaler(true)

                // Step 1: Initialize Factory
                withContext(Dispatchers.Default) {
                    setupWebRTCFactory()
                }

                if (isDestroyed) return@launch

                // Step 2: Get ICE servers from backend
                val iceServersJson = withContext(Dispatchers.IO) {
                    initiateCallSession()
                }

                if (isDestroyed) return@launch

                // Step 3: Create PeerConnection
                setupPeerConnection(iceServersJson)

                if (isDestroyed) return@launch

                // Step 4: Start local media
                startLocalMedia()

                if (isDestroyed) return@launch

                // Step 5: Create offer if initiator
                if (isInitiator) {
                    delay(1000) // Wait for local media to be ready
                    createOffer()
                    tvCallStatus.text = "📞 Appel en cours..."
                } else {
                    waitForOffer()
                    tvCallStatus.text = "📞 Réception de l'appel..."
                }

                Log.d(TAG, "✅ WebRTC initialized successfully")

            } catch (e: Exception) {
                if (!isDestroyed) {
                    Log.e(TAG, "❌ Error initializing WebRTC", e)
                    runOnUiThread {
                        Toast.makeText(
                            this@VideoCallActivity,
                            "❌ Erreur d'initialisation: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
        }
    }
    private suspend fun initiateCallSession(): String = withContext(Dispatchers.IO) {
        try {
            val token = "Bearer ${tokenManager.getAccessToken()}"
            val request = mapOf(
                "appointmentId" to appointmentId!!,
                "callType" to callType
            )

            Log.d(TAG, "🔐 Initiating call session for appointment: $appointmentId")
            Log.d(TAG, "👤 Using DOCTOR service for WebRTC (same for patients)")

            // ✅ TOUT LE MONDE utilise le service DOCTOR pour WebRTC
            val response = RetrofitClient.getWebRTCService(this@VideoCallActivity)
                .initiateCall(token, request)

            if (response.isSuccessful && response.body() != null) {
                val callSession = response.body()!!
                callId = callSession.callId

                Log.d(TAG, "✅ Call session created: $callId")
                Log.d(TAG, "🧊 ICE Servers: ${callSession.iceServers}")

                return@withContext callSession.iceServers ?: "[]"
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ Failed to create call session: ${response.code()} - $errorBody")

                throw Exception("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initiating call", e)
            throw e
        }
    }

    // Helper methods to determine user role
    private fun isPatient(): Boolean {
        // You might need to adjust this based on how you determine user role
        return intent.getBooleanExtra("isPatient", true) || !isInitiator
    }

    private fun getUserRole(): String {
        return if (isPatient()) "PATIENT" else "DOCTOR"
    }
//    private suspend fun initiateCallSession(): String = withContext(Dispatchers.IO) {
//        try {
//            val token = "Bearer ${tokenManager.getAccessToken()}"
//            val request = mapOf(
//                "appointmentId" to appointmentId!!,
//                "callType" to callType
//            )
//
//            val response = RetrofitClient.getWebRTCService(this@VideoCallActivity)
//                .initiateCall(token, request)
//
//            if (response.isSuccessful && response.body() != null) {
//                val callSession = response.body()!!
//                callId = callSession.callId
//
//                Log.d(TAG, "✅ Call session created: $callId")
//                Log.d(TAG, "🧊 ICE Servers: ${callSession.iceServers}")
//
//                return@withContext callSession.iceServers ?: "[]"
//            } else {
//                throw Exception("Failed to create call session: ${response.code()}")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "❌ Error initiating call", e)
//            throw e
//        }
//    }

    private fun setupWebRTCFactory() {
        try {
            Log.d(TAG, "🏗️ Setting up WebRTC factory")

            // Initialize PeerConnectionFactory with proper options
            val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions()

            PeerConnectionFactory.initialize(initializationOptions)

            // Create encoder and decoder factories
            val encoderFactory = DefaultVideoEncoderFactory(
                eglBase?.eglBaseContext,
                true,  // enableIntelVp8Encoder
                true   // enableH264HighProfile
            )

            val decoderFactory = DefaultVideoDecoderFactory(eglBase?.eglBaseContext)

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory()

            Log.d(TAG, "✅ PeerConnectionFactory created")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting up factory", e)
            throw e
        }
    }
    private fun setupPeerConnection(iceServersJson: String?) {
        try {
            Log.d(TAG, "🔧 Setting up PeerConnection")

            val iceServers = parseIceServers(iceServersJson)

            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
                bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
                rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                keyType = PeerConnection.KeyType.ECDSA
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN // ✅ Explicit
            }

            peerConnection = peerConnectionFactory?.createPeerConnection(
                rtcConfig,
                object : PeerConnection.Observer {
                    override fun onIceCandidate(candidate: IceCandidate?) {
                        candidate?.let {
                            Log.d(TAG, "🧊 ICE candidate: ${it.sdp}")
                            sendIceCandidate(it)
                        }
                    }

                    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                        Log.d(TAG, "❄️ ICE State: $state")
                        runOnUiThread {
                            when (state) {
                                PeerConnection.IceConnectionState.CONNECTED -> {
                                    tvCallStatus.text = "✅ Connecté"
                                    Toast.makeText(this@VideoCallActivity, "✅ Connecté au patient", Toast.LENGTH_SHORT).show()
                                }
                                PeerConnection.IceConnectionState.DISCONNECTED -> {
                                    tvCallStatus.text = "⚠️ Déconnecté"
                                }
                                PeerConnection.IceConnectionState.FAILED -> {
                                    tvCallStatus.text = "❌ Échec"
                                    Toast.makeText(
                                        this@VideoCallActivity,
                                        "Connexion échouée",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                PeerConnection.IceConnectionState.CHECKING -> {
                                    tvCallStatus.text = "🔄 Connexion..."
                                }
                                else -> {}
                            }
                        }
                    }

                    // ✅ REMPLACER onAddStream par onAddTrack
                    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                        Log.d(TAG, "📹 Remote track added")
                        receiver?.track()?.let { track ->
                            when (track.kind()) {
                                "video" -> {
                                    val videoTrack = track as VideoTrack
                                    runOnUiThread {
                                        videoTrack.addSink(remoteSurfaceView)
                                        Toast.makeText(this@VideoCallActivity, "📹 Patient connecté", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                "audio" -> {
                                    Log.d(TAG, "🔊 Remote audio track added")
                                }
                                else -> {
                                    Log.d(TAG, "⚠️ Unknown track type: ${track.kind()}")
                                }
                            }
                        }
                    }

                    override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                        Log.d(TAG, "📡 Signaling: $state")
                    }

                    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                    override fun onRemoveStream(stream: MediaStream?) {}
                    override fun onAddStream(stream: MediaStream?) {} // ✅ Deprecated mais gardé pour compatibilité
                    override fun onRenegotiationNeeded() {}
                    override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                        Log.d(TAG, "❄️ ICE Gathering: $state")
                    }
                    override fun onDataChannel(dataChannel: DataChannel?) {}
                }
            )

            Log.d(TAG, "✅ PeerConnection created")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting up peer connection", e)
            throw e
        }
    }
//    private fun setupPeerConnection(iceServersJson: String?) {
//        try {
//            Log.d(TAG, "🔧 Setting up PeerConnection")
//
//            val iceServers = parseIceServers(iceServersJson)
//
//            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
//                tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
//                bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
//                rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
//                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
//                keyType = PeerConnection.KeyType.ECDSA
//            }
//
//            peerConnection = peerConnectionFactory?.createPeerConnection(
//                rtcConfig,
//                object : PeerConnection.Observer {
//                    override fun onIceCandidate(candidate: IceCandidate?) {
//                        candidate?.let {
//                            Log.d(TAG, "🧊 ICE candidate: ${it.sdp}")
//                            sendIceCandidate(it)
//                        }
//                    }
//
//                    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
//                        Log.d(TAG, "❄️ ICE State: $state")
//                        runOnUiThread {
//                            when (state) {
//                                PeerConnection.IceConnectionState.CONNECTED -> {
//                                    tvCallStatus.text = "✅ Connecté"
//                                    Toast.makeText(this@VideoCallActivity, "✅ Connecté au patient", Toast.LENGTH_SHORT).show()
//                                }
//                                PeerConnection.IceConnectionState.DISCONNECTED -> {
//                                    tvCallStatus.text = "⚠️ Déconnecté"
//                                }
//                                PeerConnection.IceConnectionState.FAILED -> {
//                                    tvCallStatus.text = "❌ Échec"
//                                    Toast.makeText(
//                                        this@VideoCallActivity,
//                                        "Connexion échouée",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                }
//                                PeerConnection.IceConnectionState.CHECKING -> {
//                                    tvCallStatus.text = "🔄 Connexion..."
//                                }
//                                else -> {}
//                            }
//                        }
//                    }
//
//                    override fun onAddStream(stream: MediaStream?) {
//                        Log.d(TAG, "📹 Remote stream added: ${stream?.id}")
//                        runOnUiThread {
//                            stream?.videoTracks?.firstOrNull()?.addSink(remoteSurfaceView)
//                            Toast.makeText(this@VideoCallActivity, "📹 Patient connecté", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                    override fun onSignalingChange(state: PeerConnection.SignalingState?) {
//                        Log.d(TAG, "📡 Signaling: $state")
//                    }
//
//                    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
//                    override fun onRemoveStream(stream: MediaStream?) {}
//                    override fun onRenegotiationNeeded() {}
//                    override fun onIceConnectionReceivingChange(receiving: Boolean) {}
//                    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
//                        Log.d(TAG, "❄️ ICE Gathering: $state")
//                    }
//                    override fun onDataChannel(dataChannel: DataChannel?) {}
//                    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
//                }
//            )
//
//            Log.d(TAG, "✅ PeerConnection created")
//        } catch (e: Exception) {
//            Log.e(TAG, "❌ Error setting up peer connection", e)
//            throw e
//        }
//    }

    private fun parseIceServers(json: String?): List<PeerConnection.IceServer> {
        val servers = mutableListOf<PeerConnection.IceServer>()

        try {
            if (!json.isNullOrEmpty()) {
                val jsonArray = JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    val server = jsonArray.getJSONObject(i)
                    val urls = when {
                        server.has("urls") -> server.getString("urls")
                        server.has("url") -> server.getString("url")
                        else -> continue
                    }

                    if (server.has("username") && server.has("credential")) {
                        servers.add(
                            PeerConnection.IceServer.builder(urls)
                                .setUsername(server.getString("username"))
                                .setPassword(server.getString("credential"))
                                .createIceServer()
                        )
                    } else {
                        servers.add(
                            PeerConnection.IceServer.builder(urls)
                                .createIceServer()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing ICE servers", e)
        }

        // Fallback to public STUN servers
        if (servers.isEmpty()) {
            servers.addAll(listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
            ))
        }

        Log.d(TAG, "🧊 Parsed ${servers.size} ICE servers")
        return servers
    }
//    private fun startLocalMedia() {
//        try {
//            Log.d(TAG, "🎥 Starting local media")
//
//            // Create audio source first
//            val audioConstraints = MediaConstraints()
//            audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
//            localAudioTrack = peerConnectionFactory?.createAudioTrack("local_audio", audioSource)
//
//            // ✅ Add audio track directly (pas de stream)
//            localAudioTrack?.let {
//                peerConnection?.addTrack(it, listOf("local_stream"))
//            }
//
//            if (callType == "VIDEO") {
//                // Create video source and capturer
//                videoSource = peerConnectionFactory?.createVideoSource(false)
//                videoCapturer = createVideoCapturer()
//
//                videoCapturer?.let { capturer ->
//                    val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase?.eglBaseContext)
//                    capturer.initialize(surfaceTextureHelper, applicationContext, videoSource?.capturerObserver)
//                    capturer.startCapture(640, 480, 30)
//                }
//
//                localVideoTrack = peerConnectionFactory?.createVideoTrack("local_video", videoSource)
//                localVideoTrack?.addSink(localSurfaceView)
//
//                // ✅ Add video track directly (pas de stream)
//                localVideoTrack?.let {
//                    peerConnection?.addTrack(it, listOf("local_stream"))
//                }
//
//                Log.d(TAG, "✅ Video track created and started")
//            }
//
//            Log.d(TAG, "✅ Local media started successfully")
//        } catch (e: Exception) {
//            Log.e(TAG, "❌ Error starting local media", e)
//            throw e
//        }
//    }
private fun startLocalMedia() {
    try {
        Log.d(TAG, "🎥 Starting local media - Patient: ${isPatient()}")

        // Create audio source first
        val audioConstraints = MediaConstraints()
        audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("local_audio", audioSource)

        // ✅ Add audio track to peer connection
        localAudioTrack?.let { audioTrack ->
            val streamId = "local_stream_audio"
            peerConnection?.addTrack(audioTrack, listOf(streamId))
            Log.d(TAG, "✅ Audio track added to peer connection")
        }

        if (callType == "VIDEO") {
            // Create video source and capturer
            videoSource = peerConnectionFactory?.createVideoSource(false)
            videoCapturer = createVideoCapturer()

            videoCapturer?.let { capturer ->
                val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase?.eglBaseContext)
                capturer.initialize(surfaceTextureHelper, applicationContext, videoSource?.capturerObserver)
                capturer.startCapture(640, 480, 30)

                Log.d(TAG, "✅ Video capturer started")
            }

            localVideoTrack = peerConnectionFactory?.createVideoTrack("local_video", videoSource)

            // ✅ CRITICAL: Add sink to local surface view BEFORE adding to peer connection
            localVideoTrack?.addSink(localSurfaceView)
            Log.d(TAG, "✅ Video track sink added to local surface")

            // ✅ Add video track to peer connection
            localVideoTrack?.let { videoTrack ->
                val streamId = "local_stream_video"
                peerConnection?.addTrack(videoTrack, listOf(streamId))
                Log.d(TAG, "✅ Video track added to peer connection")
            }

            // ✅ Force surface view to be visible and request layout
            runOnUiThread {
                localSurfaceView.visibility = View.VISIBLE
                localSurfaceView.requestLayout()
                Log.d(TAG, "✅ Local surface view made visible")
            }
        }

        Log.d(TAG, "✅ Local media started successfully")

    } catch (e: Exception) {
        Log.e(TAG, "❌ Error starting local media", e)
        runOnUiThread {
            Toast.makeText(this, "❌ Camera error: ${e.message}", Toast.LENGTH_LONG).show()
        }
        throw e
    }
}
//    private fun startLocalMedia() {
//        try {
//            Log.d(TAG, "🎥 Starting local media")
//
//            // Create audio source first
//            val audioConstraints = MediaConstraints()
//            audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
//            localAudioTrack = peerConnectionFactory?.createAudioTrack("local_audio", audioSource)
//
//            if (callType == "VIDEO") {
//                // Create video source and capturer
//                videoSource = peerConnectionFactory?.createVideoSource(false)
//                videoCapturer = createVideoCapturer()
//
//                videoCapturer?.let { capturer ->
//                    val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase?.eglBaseContext)
//                    capturer.initialize(surfaceTextureHelper, applicationContext, videoSource?.capturerObserver)
//                    capturer.startCapture(640, 480, 30) // Start with lower resolution for stability
//                }
//
//                localVideoTrack = peerConnectionFactory?.createVideoTrack("local_video", videoSource)
//                localVideoTrack?.addSink(localSurfaceView)
//
//                Log.d(TAG, "✅ Video track created and started")
//            }
//
//            // Add tracks to peer connection
//            val localStream = peerConnectionFactory?.createLocalMediaStream("local_stream")
//            localAudioTrack?.let { localStream?.addTrack(it) }
//            if (callType == "VIDEO") {
//                localVideoTrack?.let { localStream?.addTrack(it) }
//            }
//
//            peerConnection?.addStream(localStream)
//
//            Log.d(TAG, "✅ Local media started successfully")
//        } catch (e: Exception) {
//            Log.e(TAG, "❌ Error starting local media", e)
//            throw e
//        }
//    }

    private fun createVideoCapturer(): CameraVideoCapturer? {
        return try {
            val enumerator = Camera2Enumerator(this)
            val deviceNames = enumerator.deviceNames

            Log.d(TAG, "📷 Available cameras: ${deviceNames.joinToString()}")

            // Try front camera first
            for (deviceName in deviceNames) {
                if (enumerator.isFrontFacing(deviceName)) {
                    Log.d(TAG, "📷 Using front camera: $deviceName")
                    return enumerator.createCapturer(deviceName, null)
                }
            }

            // Fallback to any camera
            for (deviceName in deviceNames) {
                Log.d(TAG, "📷 Using camera: $deviceName")
                return enumerator.createCapturer(deviceName, null)
            }

            Log.e(TAG, "❌ No camera found")
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating video capturer", e)
            null
        }
    }

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
                    Log.d(TAG, "✅ Offer created: ${it.description.substring(0, 50)}...")
                    peerConnection?.setLocalDescription(SimpleSdpObserver(), it)
                    sendOfferSdp(it.description)
                }
            }

            override fun onSetSuccess() {
                Log.d(TAG, "✅ Local description set successfully")
            }

            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "❌ Create offer failed: $error")
                runOnUiThread {
                    Toast.makeText(this@VideoCallActivity, "❌ Erreur création appel", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "❌ Set description failed: $error")
            }
        }, constraints)
    }

    private fun sendOfferSdp(sdp: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val body = mapOf("sdp" to sdp)

                val response = RetrofitClient.getWebRTCService(this@VideoCallActivity)
                    .sendOffer(token, callId!!, body)

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Offer sent successfully")
                    withContext(Dispatchers.Main) {
                        tvCallStatus.text = "📞 En attente du patient..."
                        waitForAnswer()
                    }
                } else {
                    Log.e(TAG, "❌ Failed to send offer: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending offer", e)
            }
        }
    }

    private fun waitForAnswer() {
        lifecycleScope.launch(Dispatchers.IO) {
            repeat(60) { // 2 minutes timeout
                if (isDestroyed) return@launch
                delay(2000)

                try {
                    val token = "Bearer ${tokenManager.getAccessToken()}"
                    val response = RetrofitClient.getWebRTCService(this@VideoCallActivity)
                        .getCallSession(token, callId!!)

                    if (response.isSuccessful && response.body() != null) {
                        val session = response.body()!!
                        if (!session.answerSdp.isNullOrEmpty()) {
                            Log.d(TAG, "✅ Answer received")
                            withContext(Dispatchers.Main) {
                                receiveAnswerSdp(session.answerSdp)
                            }
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error polling answer", e)
                }
            }

            // Timeout
            withContext(Dispatchers.Main) {
                Toast.makeText(this@VideoCallActivity, "❌ Patient non joignable", Toast.LENGTH_SHORT).show()
                endCall()
            }
        }
    }

    private fun receiveAnswerSdp(sdp: String) {
        val answerSdp = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection?.setRemoteDescription(SimpleSdpObserver(), answerSdp)
        Log.d(TAG, "✅ Remote description set from answer")
    }

    private fun waitForOffer() {
        lifecycleScope.launch(Dispatchers.IO) {
            repeat(60) { // 2 minutes timeout
                if (isDestroyed) return@launch
                delay(2000)

                try {
                    val token = "Bearer ${tokenManager.getAccessToken()}"
                    val response = RetrofitClient.getWebRTCService(this@VideoCallActivity)
                        .getCallSession(token, callId!!)

                    if (response.isSuccessful && response.body() != null) {
                        val session = response.body()!!
                        if (!session.offerSdp.isNullOrEmpty()) {
                            Log.d(TAG, "✅ Offer received")
                            withContext(Dispatchers.Main) {
                                receiveOfferSdp(session.offerSdp)
                            }
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error polling offer", e)
                }
            }
        }
    }

    private fun receiveOfferSdp(sdp: String) {
        val offerSdp = SessionDescription(SessionDescription.Type.OFFER, sdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                createAnswer()
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "❌ Set remote failed: $error")
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
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun sendAnswerSdp(sdp: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val body = mapOf("sdp" to sdp)

                RetrofitClient.getWebRTCService(this@VideoCallActivity)
                    .sendAnswer(token, callId!!, body)

                Log.d(TAG, "✅ Answer sent")
                withContext(Dispatchers.Main) {
                    tvCallStatus.text = "✅ Appel en cours"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending answer", e)
            }
        }
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val body = mapOf(
                    "candidate" to candidate.sdp,
                    "sdpMid" to candidate.sdpMid,
                    "sdpMLineIndex" to candidate.sdpMLineIndex
                )

                RetrofitClient.getWebRTCService(this@VideoCallActivity)
                    .sendIceCandidate(token, callId!!, body)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending ICE", e)
            }
        }
    }

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
        if (callType != "VIDEO") return

        isVideoEnabled = !isVideoEnabled
        localVideoTrack?.setEnabled(isVideoEnabled)

        runOnUiThread {
            localSurfaceView.visibility = if (isVideoEnabled) View.VISIBLE else View.INVISIBLE
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
        if (callType != "VIDEO") return

        videoCapturer?.let {
            try {
                it.switchCamera(null)
                Toast.makeText(this, "🔄 Caméra changée", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error switching camera", e)
                Toast.makeText(this, "❌ Erreur changement caméra", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun endCall() {
        runOnUiThread {
            tvCallStatus.text = "📞 Fin d'appel..."
            Toast.makeText(this, "📞 Appel terminé", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            try {
                if (callId != null) {
                    val token = "Bearer ${tokenManager.getAccessToken()}"
                    val body = mapOf("reason" to "COMPLETED")

                    withContext(Dispatchers.IO) {
                        try {
                            RetrofitClient.getWebRTCService(this@VideoCallActivity)
                                .endCall(token, callId!!, body)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error ending call on server", e)
                        }
                    }
                }
            } finally {
                cleanup()
                finish()
            }
        }
    }

    private fun cleanup() {
        isDestroyed = true

        try {
            localVideoTrack?.removeSink(localSurfaceView)
            localVideoTrack?.dispose()
            localAudioTrack?.dispose()

            videoCapturer?.let {
                try {
                    it.stopCapture()
                    it.dispose()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error stopping capturer", e)
                }
            }

            videoSource?.dispose()
            audioSource?.dispose()

            peerConnection?.close()
            peerConnection?.dispose()

            localSurfaceView.release()
            remoteSurfaceView.release()

            eglBase?.release()

            peerConnectionFactory?.dispose()

            // Remove keep screen on flag
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            Log.d(TAG, "✅ Cleanup complete")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cleanup error", e)
        }
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Prevent back button from ending call accidentally
        Toast.makeText(this, "Utilisez le bouton 'Terminer' pour quitter l'appel", Toast.LENGTH_SHORT).show()
        super.onBackPressed() // ✅ Added super call
    }

    // Inner class for SimpleSdpObserver
    private inner class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {
            // Optional: Add implementation if needed
        }

        override fun onSetSuccess() {
            // Optional: Add implementation if needed
        }

        override fun onCreateFailure(error: String?) {
            Log.e(TAG, "SDP creation failed: $error")
        }

        override fun onSetFailure(error: String?) {
            Log.e(TAG, "SDP set failed: $error")
        }
    }
}