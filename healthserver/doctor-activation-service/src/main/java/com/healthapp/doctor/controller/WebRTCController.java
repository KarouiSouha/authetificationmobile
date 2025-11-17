package com.healthapp.doctor.controller;

import com.healthapp.doctor.dto.request.InitiateCallRequest;
import com.healthapp.doctor.dto.request.AnswerCallRequest;
import com.healthapp.doctor.dto.request.IceCandidateRequest;
import com.healthapp.doctor.dto.response.CallSessionResponse;
import com.healthapp.doctor.service.WebRTCService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * WebRTC Controller - Gestion des appels vidéo/audio
 * 
 * Flow:
 * 1. Doctor/Patient clique "Appeler" -> POST /initiate
 * 2. Backend crée CallSession + retourne ICE servers
 * 3. Initiateur crée offer SDP -> POST /offer
 * 4. Destinataire reçoit notif -> GET /session/{id}
 * 5. Destinataire crée answer SDP -> POST /answer
 * 6. Les deux échangent ICE candidates -> POST /ice
 * 7. Connexion WebRTC établie (P2P ou via TURN)
 * 8. Fin d'appel -> POST /end
 */
@RestController
@RequestMapping("/api/webrtc")
@RequiredArgsConstructor
@Slf4j
public class WebRTCController {

    private final WebRTCService webRTCService;

    /**
     * STEP 1: Initier un appel
     * Appelé par le docteur ou le patient
     */
    @PostMapping("/initiate")
    public ResponseEntity<CallSessionResponse> initiateCall(
            @RequestBody InitiateCallRequest request,
            Authentication auth) {

        String initiatorEmail = auth.getName();
        log.info("📞 {} initiating {} call for appointment {}",
                initiatorEmail, request.getCallType(), request.getAppointmentId());

        CallSessionResponse session = webRTCService.initiateCall(
                request.getAppointmentId(),
                request.getCallType(),
                initiatorEmail
        );

        return ResponseEntity.ok(session);
    }

    /**
     * STEP 2: Envoyer l'offre SDP (créée par l'initiateur)
     */
    @PostMapping("/{callId}/offer")
    public ResponseEntity<Map<String, String>> sendOffer(
            @PathVariable String callId,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        String sdp = body.get("sdp");
        log.info("📤 Offer SDP received for call {}", callId);

        webRTCService.saveOfferSdp(callId, sdp);

        // TODO: Envoyer notification push au destinataire

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Offer SDP saved"
        ));
    }

    /**
     * STEP 3: Récupérer les détails d'un appel (par le destinataire)
     */
    @GetMapping("/{callId}")
    public ResponseEntity<CallSessionResponse> getCallSession(
            @PathVariable String callId,
            Authentication auth) {

        log.info("🔍 Fetching call session: {}", callId);

        CallSessionResponse session = webRTCService.getCallSession(callId);

        return ResponseEntity.ok(session);
    }

    /**
     * STEP 4: Envoyer l'answer SDP (créée par le destinataire)
     */
    @PostMapping("/{callId}/answer")
    public ResponseEntity<Map<String, String>> sendAnswer(
            @PathVariable String callId,
            @RequestBody AnswerCallRequest request,
            Authentication auth) {

        log.info("📥 Answer SDP received for call {}", callId);

        webRTCService.saveAnswerSdp(callId, request.getSdp());
        webRTCService.markCallAsActive(callId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Answer SDP saved, call is now active"
        ));
    }

    /**
     * STEP 5: Échanger ICE candidates
     * Appelé par les deux parties pendant la négociation
     */
    @PostMapping("/{callId}/ice")
    public ResponseEntity<Map<String, String>> addIceCandidate(
            @PathVariable String callId,
            @RequestBody IceCandidateRequest request,
            Authentication auth) {

        log.debug("🧊 ICE candidate received for call {}", callId);

        // En production, utilisez WebSocket pour relay ICE en temps réel
        // Ici on logge juste pour l'audit

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "ICE candidate received"
        ));
    }

    /**
     * STEP 6: Terminer l'appel
     */
    @PostMapping("/{callId}/end")
    public ResponseEntity<Map<String, String>> endCall(
            @PathVariable String callId,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        String reason = body.getOrDefault("reason", "COMPLETED");
        log.info("📵 Ending call {}: {}", callId, reason);

        webRTCService.endCall(callId, reason);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Call ended"
        ));
    }

    /**
     * Récupérer la qualité de l'appel (QoS)
     */
    @GetMapping("/{callId}/qos")
    public ResponseEntity<Map<String, Object>> getCallQuality(
            @PathVariable String callId,
            Authentication auth) {

        log.info("📊 Fetching QoS for call {}", callId);

        Map<String, Object> qos = webRTCService.getCallQuality(callId);

        return ResponseEntity.ok(qos);
    }
}