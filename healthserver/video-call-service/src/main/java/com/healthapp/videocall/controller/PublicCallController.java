package com.healthapp.videocall.controller;

import com.healthapp.videocall.dto.CallSessionResponse;
import com.healthapp.videocall.service.CallSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * PublicCallController - Endpoints publics pour video calls
 * Accessible sans authentification stricte (JWT validé mais pas de rôle requis)
 */
@RestController
@RequestMapping("/api/public/calls")
@RequiredArgsConstructor
@Slf4j
public class PublicCallController {
    
    private final CallSessionService callSessionService;
    
    /**
     * ✅ NOUVEAU: Patient crée un appel d'urgence
     * POST /api/public/calls/emergency
     */
    @PostMapping("/emergency")
    public ResponseEntity<Map<String, Object>> createEmergencyCall(
            @RequestBody Map<String, String> request) {
        
        String patientId = request.get("patientId");
        String patientEmail = request.get("patientEmail");
        String patientName = request.get("patientName");
        String alertType = request.get("alertType");
        
        log.info("🚨 Creating EMERGENCY call for patient: {}", patientEmail);
        
        // Créer un appointmentId temporaire
        String tempAppointmentId = "EMERGENCY_" + System.currentTimeMillis();
        
        // Trouver un docteur disponible (simplifié)
        String doctorId = "doctor_on_call";
        String doctorEmail = "doctor@example.com";
        
        CallSessionResponse session = callSessionService.createCallSession(
            tempAppointmentId,
            patientId,
            patientEmail,
            doctorId,
            doctorEmail
        );
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "callId", session.getCallId(),
            "sessionId", session.getCallId(),
            "appointmentId", tempAppointmentId,
            "doctorId", doctorId,
            "iceServers", session.getIceServers(),
            "signalingUrl", "http://localhost:8087/api/public/calls/signaling/" + session.getCallId()
        ));
    }
    
    /**
     * ✅ Doctor rejoint un appel
     * POST /api/public/calls/{sessionId}/join
     */
    @PostMapping("/{sessionId}/join")
    public ResponseEntity<Map<String, Object>> joinCall(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request) {
        
        String doctorId = request.get("doctorId");
        String doctorEmail = request.get("doctorEmail");
        
        log.info("👨‍⚕️ Doctor {} joining call: {}", doctorEmail, sessionId);
        
        // Récupérer la session
        CallSessionResponse session = callSessionService.getSession(sessionId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "sessionId", sessionId,
            "iceServers", session.getIceServers(),
            "patientId", session.getPatientId()
        ));
    }
    
    /**
     * ✅ SIGNALING: Envoyer une offre SDP
     * POST /api/public/calls/signaling/{sessionId}/offer
     */
    @PostMapping("/signaling/{sessionId}/offer")
    public ResponseEntity<Map<String, Object>> sendOffer(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> offer) {
        
        log.info("📤 Received OFFER for session: {}", sessionId);
        
        String sdp = (String) offer.get("sdp");
        String senderId = (String) offer.get("senderId");
        
        // Sauvegarder l'offre
        callSessionService.saveOffer(sessionId, sdp);
        
        // Stocker temporairement pour que l'autre peer puisse récupérer
        callSessionService.storeSignalingData(sessionId, "offer", offer);
        
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    /**
     * ✅ SIGNALING: Envoyer une réponse SDP
     * POST /api/public/calls/signaling/{sessionId}/answer
     */
    @PostMapping("/signaling/{sessionId}/answer")
    public ResponseEntity<Map<String, Object>> sendAnswer(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> answer) {
        
        log.info("📤 Received ANSWER for session: {}", sessionId);
        
        String sdp = (String) answer.get("sdp");
        
        callSessionService.saveAnswer(sessionId, sdp);
        callSessionService.storeSignalingData(sessionId, "answer", answer);
        
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    /**
     * ✅ SIGNALING: Envoyer un ICE candidate
     * POST /api/public/calls/signaling/{sessionId}/ice
     */
    @PostMapping("/signaling/{sessionId}/ice")
    public ResponseEntity<Map<String, Object>> sendIceCandidate(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> ice) {
        
        log.debug("🧊 Received ICE candidate for session: {}", sessionId);
        
        callSessionService.storeIceCandidate(sessionId, ice);
        
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    /**
     * ✅ SIGNALING: Récupérer l'offre (polling)
     * GET /api/public/calls/signaling/{sessionId}/offer
     */
    @GetMapping("/signaling/{sessionId}/offer")
    public ResponseEntity<Map<String, Object>> getOffer(@PathVariable String sessionId) {
        Map<String, Object> offer = callSessionService.getSignalingData(sessionId, "offer");
        
        if (offer != null) {
            return ResponseEntity.ok(offer);
        }
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * ✅ SIGNALING: Récupérer la réponse (polling)
     * GET /api/public/calls/signaling/{sessionId}/answer
     */
    @GetMapping("/signaling/{sessionId}/answer")
    public ResponseEntity<Map<String, Object>> getAnswer(@PathVariable String sessionId) {
        Map<String, Object> answer = callSessionService.getSignalingData(sessionId, "answer");
        
        if (answer != null) {
            return ResponseEntity.ok(answer);
        }
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * ✅ SIGNALING: Récupérer les ICE candidates (polling)
     * GET /api/public/calls/signaling/{sessionId}/ice
     */
    @GetMapping("/signaling/{sessionId}/ice")
    public ResponseEntity<Map<String, Object>> getIceCandidates(@PathVariable String sessionId) {
        Map<String, Object> iceData = callSessionService.getIceCandidates(sessionId);
        
        return ResponseEntity.ok(iceData);
    }
}